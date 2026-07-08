package handler;

import bean.FundBean;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TianTianFundHandler extends FundRefreshHandler {
    public final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Gson gson = new Gson();
    private static final Pattern JSONP_PATTERN = Pattern.compile("jsonpgz\\((\\{.*\\})\\);");

    private JLabel refreshTimeLabel;

    public TianTianFundHandler(JTable table, JLabel refreshTimeLabel) {
        super(table);
        this.refreshTimeLabel = refreshTimeLabel;
    }

    @Override
    public void handle(List<String> code) {
        //LogUtil.info("Leeks 更新Fund编码数据.");

        if (code.isEmpty()) {
            return;
        }

        stepAction(code);
    }

    @Override
    public void stopHandle() {
        LogUtil.info("Leeks 准备停止更新Fund编码数据.");
    }

    private void stepAction(List<String> codes) {
//        LogUtil.info("Leeks 刷新基金数据.");
        List<String> codeList = new ArrayList<>();
        Map<String, String[]> codeMap = new HashMap<>();
        for (String str : codes) {
            //兼容原有设置
            String[] strArray;
            if (str.contains(",")) {
                strArray = str.split(",");
            } else {
                strArray = new String[]{str};
            }
            codeList.add(strArray[0]);
            codeMap.put(strArray[0], strArray);
        }

        for (String code : codeList) {
            new Thread(() -> {
                try {
                    String result = HttpClientPool.getHttpClient().get("http://fundgz.1234567.com.cn/js/" + code + ".js?rt=" + System.currentTimeMillis());
                    if (result == null || result.isEmpty()) {
                        LogUtil.info("Fund编码:[" + code + "]请求无响应");
                        return;
                    }
                    Matcher m = JSONP_PATTERN.matcher(result);
                    if (!m.find()) {
                        LogUtil.info("Fund编码:[" + code + "]返回格式异常:" + result);
                        return;
                    }
                    String json = m.group(1);
                    if (!json.isEmpty()) {
                        FundBean bean = gson.fromJson(json, FundBean.class);
                        FundBean.loadFund(bean, codeMap);

                        BigDecimal gszDec = new BigDecimal(bean.getGsz());

                        String costPriceStr = bean.getCostPrise();
                        if (StringUtils.isNotEmpty(costPriceStr)) {
                            BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                            BigDecimal incomeDiff = gszDec.add(costPriceDec.negate());
                            if (costPriceDec.compareTo(BigDecimal.ZERO) <= 0) {
                                bean.setIncomePercent("0");
                            } else {
                                BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 8, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.TEN)
                                        .multiply(BigDecimal.TEN)
                                        .setScale(3, RoundingMode.HALF_UP);
                                bean.setIncomePercent(incomePercentDec.toString());
                            }

                            String bondStr = bean.getBonds();
                            if (StringUtils.isNotEmpty(bondStr)) {
                                BigDecimal bondDec = new BigDecimal(bondStr);
                                BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                                        .setScale(2, RoundingMode.HALF_UP);
                                bean.setIncome(incomeDec.toString());
                            }
                        }

                        String dwjzStr = bean.getDwjz();
                        if (StringUtils.isNotEmpty(dwjzStr)) {
                            String bondStr = bean.getBonds();
                            if (StringUtils.isNotEmpty(bondStr)) {
                                BigDecimal dwjzDec = new BigDecimal(dwjzStr);
                                BigDecimal todayDiff = gszDec.add(dwjzDec.negate());
                                BigDecimal bondDec = new BigDecimal(bondStr);
                                BigDecimal todayIncomeDec = todayDiff.multiply(bondDec)
                                        .setScale(2, RoundingMode.HALF_UP);
                                bean.setTodayIncome(todayIncomeDec.toString());
                            }
                        }

                        updateData(bean);
                    } else {
                        LogUtil.info("Fund编码:[" + code + "]无法获取数据");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        updateUI();
    }

    public void updateUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshTimeLabel.setText(LocalDateTime.now().format(timeFormatter));
                refreshTimeLabel.setToolTipText("最后刷新时间");
            }
        });
    }

}

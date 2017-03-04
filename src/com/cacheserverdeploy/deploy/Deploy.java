package com.cacheserverdeploy.deploy;


import java.util.HashMap;
import java.util.Map;

public class Deploy {
    /**
     * 你需要完成的入口
     * <功能详细描述>
     *
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */

    //网络节点数量
    private static int netNodeCount;
    //网络链路数量
    private static int netLinkCount;
    //消费节点数量
    private static int consumptionNodeCount;
    //视频内容服务器部署成本
    private static int deploymentCost;

    //网络链路图的带权图的邻接矩阵
    private static Weight [][] graph;

    /*消费节点与相连网络节点的信息存储
      存储的格式如下：Map<Integer,ConsumptionNodeInfo>
      map中key是消费节点，value是相连的网络节点的ID与需要的带宽
     */
    private static Map<Integer,ConsumptionNodeInfo> consumptionInfo;

    public static String[] deployServer(String[] graphContent) {
        /**do your work here**/
        initData(graphContent);
        System.out.println(consumptionInfo.get(8).getLinkedID());
        return new String[]{"17", "\r\n", "0 8 0 20"};
    }


    /**
     * 处理图文本信息，构造图
     * @param graphContent 图文本信息
     */
    private static void initData(String[] graphContent) {

        //处理网络节点数量,成本等信息
        String [] firstLine = lineTos(graphContent[0]);
        netNodeCount = sToi(firstLine[0]);
        netLinkCount = sToi(firstLine[1]);
        consumptionNodeCount = sToi(firstLine[2]);

        String [] thirdLine = lineTos(graphContent[2]);
        deploymentCost = sToi(thirdLine[0]);

        //构造图
        graph = new Weight[netNodeCount][netNodeCount];

        //初始化图中的权值信息，默认为null
        for (int i = 0 ; i < netNodeCount; i ++) {
            for (int j = 0 ; j < netNodeCount;j++) {
                graph[i][j] = null;
            }
        }

        //判断网络节点的链接信息 是否读完
        boolean isNotEnd = true;
        //网络节点信息读取开启的行数
        int lineNum = 4;

        while (isNotEnd) {
            String [] edgeInfo = lineTos(graphContent[lineNum++]);
            //设置起点
            int originID = sToi(edgeInfo[0]);
            //设置终点
            int endID = sToi(edgeInfo[1]);

            //设置权重
            Weight weight = new Weight();
            weight.setTotalBandwidth(sToi(edgeInfo[2]));
            weight.setNetRentCost(sToi(edgeInfo[3]));
            graph[originID][endID] = weight;

            if (lineTos(graphContent[lineNum]).length == 0) {
                isNotEnd = false;
            }
        }

        //读取消费节点信息
        lineNum++;
        int count = consumptionNodeCount;
        consumptionInfo = new HashMap<>(consumptionNodeCount);

        while (count-- > 0) {
            String[] infos = lineTos(graphContent[lineNum++]);
            int consumptionID = sToi(infos[0]);
            int linkedID = sToi(infos[1]);
            int requiredBandWidth = sToi(infos[2]);

            ConsumptionNodeInfo info = new ConsumptionNodeInfo();
            info.setLinkedID(linkedID);
            info.setRequiredBandWidth(requiredBandWidth);
            consumptionInfo.put(consumptionID,info);
        }

    }




    /**
     *字符串转换成整数
     * @param s 字符串
     * @return i 转换成整数
     */
    private static int sToi(String s) {
        return Integer.valueOf(s);
    }

    /**
     * 一行的字符串分割成一个字符串数组
     * @param line 一行的字符串
     * @return 分割后的字符串数组
     */
    private static String[] lineTos(String line) {
        if ("".equals(line)) {
            return new String[]{};
        }
        return line.split(" ");
    }


    /**
     * 网络节点链路图的类
     * 图中边的权值信息，包括总带宽费用，网络租用费
     */
    private static class Weight{
        //总带宽费用
        private int totalBandwidth;
        //网络租用费
        private int netRentCost;

        public int getTotalBandwidth() {
            return totalBandwidth;
        }

        public void setTotalBandwidth(int totalBandwidth) {
            this.totalBandwidth = totalBandwidth;
        }

        public int getNetRentCost() {
            return netRentCost;
        }

        public void setNetRentCost(int netRentCost) {
            this.netRentCost = netRentCost;

        }

    }

    /**
     * 消费节点的相关信息
     * 与之相连的ID
     * 需求的带宽
     */
    private static class ConsumptionNodeInfo{
        //相连的ID
        private int linkedID;
        //需求的带宽
        private int requiredBandWidth;


        public int getLinkedID() {
            return linkedID;
        }

        public void setLinkedID(int linkedID) {
            this.linkedID = linkedID;
        }

        public int getRequiredBandWidth() {
            return requiredBandWidth;
        }

        public void setRequiredBandWidth(int requiredBandWidth) {
            this.requiredBandWidth = requiredBandWidth;
        }
    }


}

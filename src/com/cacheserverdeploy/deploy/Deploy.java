package com.cacheserverdeploy.deploy;


import java.util.*;

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
    private static Weight[][] graph;

    /*消费节点与相连网络节点的信息存储
      存储的格式如下：Map<Integer,ConsumptionNodeInfo>
      map中key是消费节点，value是相连的网络节点的ID与需要的带宽
     */
    private static Map<Integer, ConsumptionNodeInfo> consumptionInfo;

    //表示不可达的常亮
    private static Weight NO_PATH_WEIGHT = new Weight();

    static {
        NO_PATH_WEIGHT.setTotalBandwidth(0);
        NO_PATH_WEIGHT.setNetRentCost(101);
    }

    public static String[] deployServer(String[] graphContent) {
        /**do your work here**/
        initData(graphContent);
//        printMST(0);
        String [] result = new String[netNodeCount + 2];
        result[0] = consumptionNodeCount + "";
        result[1] = "\r\n";
        int count = 2;
        for (int i = 0 ; i < consumptionNodeCount;i++) {
            result[count++] = consumptionInfo.get(i).getLinkedID() + " " + (i) + " " + consumptionInfo.get(i).getRequiredBandWidth();
        }
        
        return result;
//        return new String[]{"17", "\r\n", "0 8 0 20"};
    }

    /**
     * 生成最小生成树（以邻接矩阵存储）
     * 生成树的策略从一个点 看其相连的边，在边的全值中，选择 网络租用费用最低，流量最大。
     * 生成树的结构 由实现人决定
     *
     * @param start
     * @return 返回生成树
     */
    public static Map<Integer, ArrayList<Integer>> createMST(int start) {

        //保存当前生成树到到剩余各顶点最小的weight值
        Weight[] lowcost = new Weight[netNodeCount];
        //顶点i是否被并入生成树中
        int[] vset = new int[netNodeCount];
        //最小值
        Weight min = NO_PATH_WEIGHT;
        //保存新加入生成树的节点
        int v = 0;
        //保存剩余顶点到当前生成树权值最小的边的顶点
        int k = 0;
//        //保存生成树的邻接矩阵
//        Weight[][] minTree = new Weight[netNodeCount][netNodeCount];
        //保存被并入生成树的节点的前驱节点（邻接点）
        int[] preset = new int[netNodeCount];

        //
        Map<Integer, ArrayList<Integer>> mst = new HashMap<>(netNodeCount);

        //初始化
        for (int i = 0; i < netNodeCount; i++) {
            lowcost[i] = graph[start][i];
            vset[i] = 0;
            preset[i] = start;
        }
        //起始节点被并入生成树
        vset[start] = 1;

        for (int m = 0; m < netNodeCount; m++) {
            ArrayList<Integer> a = new ArrayList();
            mst.put(m, a);
        }

        for (int i = 0; i < netNodeCount; i++) {

            min = NO_PATH_WEIGHT;

            //选出候选边中的最小者
            for (int j = 0; j < netNodeCount; j++)
                if (vset[j] == 0 && compareWeight(lowcost[j], min)) {
                    min = lowcost[j];
                    k = j;
                }
            //k并入生成树
            vset[k] = 1;
            //k设为中介节点
            v = k;
//                //把边k和其前驱节点连接的边保存到生成树
//                minTree[preset[k]][k] = min;

            mst.get(preset[v]).add(v);

            //检查所有节点是否并入生成树中
            int result = vset[0];
            for (int n = 1; n < netNodeCount; n++) {
                result &= vset[n];
            }
            if (result == 1) {
                break;
            }

            //以刚并入的顶点v为中介，更新候选边和某些节点的前驱节点
            for (int l = 0; l < netNodeCount; l++) {

                if (vset[l] == 0 && compareWeight(graph[v][l], lowcost[l])) {
                    lowcost[l] = graph[v][l];
                    preset[l] = v;
                }

            }


        }
        return mst;
    }

    /**
     * 比较两个weight的大小
     *
     * @return 前者比后者小，返回TRUE，否则返回FALSE
     * @param两个weight
     */
    public static boolean compareWeight(Weight weight1, Weight weight2) {

        if (weight1.getNetRentCost() < weight2.getNetRentCost()) return true;
        else if (weight1.getNetRentCost() > weight2.getNetRentCost()) return false;
        else {
            if (weight1.getTotalBandwidth() > weight2.getTotalBandwidth()) return true;
        }

        return false;
    }


    /**
     * 1. 生成树节点排序（排序规则，度从大到小）
     * 2. 在度最大的点上 添加服务器
     *
     * 3. 找出服务器到各个消费点的路径上最小的流量值
     * 4. 如果该路径的最小的流量值大于等于消费节点所需的带宽，则选择该路径，并记录
     * 5. 如果该路径的最小的流量值小于消费节点的带宽，则依旧选择该路径，消费节点所需要的带宽减去最小的流量值，并记录
     * 6.
     *
     * @param mst 一个生成树类型的变量，不是String
     * @return 所有服务器的ID
     */
    public static int[] getDeployment(Map<Integer,ArrayList<Integer>> mst) {

        return new int[]{};
    }

    /**
     * 1. 按照生成树去判断服务器能不能走到消费节点
     * 2. 如果全部能走到，则返回true
     * 3. 判断能不能走到的策略:判断服务节点去消费节点的唯一路径是否满足带宽需求，满足就能走到。
     * 4. 如果不能全部走到，保存不能走到的消费节点，假设不加服务器，加边()
     * 5. 加边的策略：消费节点所需带宽-最小生成树中的边的带宽 走费用最小的带宽最大的，从下往上走
     *
     * @param serverID 一组服务器的ID
     * @return true 如果部署的服务器能满足所有的消费节点。false 不满足
     */
    public static boolean isSatisfyAllConsumptionNode(int[] serverID) {
        return false;
    }


    /**
     * 处理图文本信息，构造图
     *
     * @param graphContent 图文本信息
     */
    private static void initData(String[] graphContent) {

        //处理网络节点数量,成本等信息
        String[] firstLine = lineTos(graphContent[0]);
        netNodeCount = sToi(firstLine[0]);
        netLinkCount = sToi(firstLine[1]);
        consumptionNodeCount = sToi(firstLine[2]);

        String[] thirdLine = lineTos(graphContent[2]);
        deploymentCost = sToi(thirdLine[0]);

        //构造图
        graph = new Weight[netNodeCount][netNodeCount];

        //初始化图中的权值信息，默认为所有节点不可达
        for (int i = 0; i < netNodeCount; i++) {
            for (int j = 0; j < netNodeCount; j++) {
                graph[i][j] = NO_PATH_WEIGHT;
            }
        }

        //判断网络节点的链接信息 是否读完
        boolean isNotEnd = true;
        //网络节点信息读取开启的行数
        int lineNum = 4;

        while (isNotEnd) {
            String[] edgeInfo = lineTos(graphContent[lineNum++]);
            //设置起点
            int originID = sToi(edgeInfo[0]);
            //设置终点
            int endID = sToi(edgeInfo[1]);

            //设置权重
            Weight weight = new Weight();
            weight.setTotalBandwidth(sToi(edgeInfo[2]));
            weight.setNetRentCost(sToi(edgeInfo[3]));
            weight.setUsedBandWidth(0);
            //无向图
            graph[originID][endID] = weight;
            graph[endID][originID] = weight;

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
            consumptionInfo.put(consumptionID, info);
        }

    }


    /**
     * 字符串转换成整数
     *
     * @param s 字符串
     * @return i 转换成整数
     */
    private static int sToi(String s) {
        return Integer.valueOf(s);
    }

    /**
     * 一行的字符串分割成一个字符串数组
     *
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
    public static class Weight {
        //总带宽费用
        private int totalBandwidth;
        //网络租用费
        private int netRentCost;
        //已用带宽
        private int usedBandWidth;

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

        public int getUsedBandWidth() {
            return usedBandWidth;
        }

        public void setUsedBandWidth(int usedBandWidth) {
            this.usedBandWidth = usedBandWidth;
        }
    }

    /**
     * 消费节点的相关信息
     * 与之相连的ID
     * 需求的带宽
     */
    private static class ConsumptionNodeInfo {
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


    public static void printMST(int start){
        Map<Integer, ArrayList<Integer>> map = createMST(start);
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ArrayList<Integer> lists = (ArrayList<Integer>) entry.getValue();
            for (int i = 0; i < lists.size(); i++) {
                System.out.println(entry.getKey() + " " + lists.get(i));
            }
//            System.out.print(entry.getKey());
//            System.out.println("--------------");
//            ArrayList<Integer> lists = (ArrayList<Integer>) entry.getValue();
//            for (int i = 0 ; i < lists.size();i++) {
//                System.out.print(lists.get(i) + " ");
//            }
//            System.out.println();
        }
    }

}

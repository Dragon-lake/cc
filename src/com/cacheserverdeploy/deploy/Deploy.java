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

    //生成树的邻接矩阵存储
    private static Weight[][] MSTgraph;

    //BFS中的顺序数组
    private static int[] orderInBFS;

    private static List<LinkedList<Integer>> paths;

    /*消费节点与相连网络节点的信息存储
      存储的格式如下：Map<Integer,ConsumptionNodeInfo>
      map中key是消费节点，value是相连的网络节点的ID与需要的带宽
     */
    private static Map<Integer, ConsumptionNodeInfo> consumptionInfo;
    //存储的是网络节点与消费节点的映射
    private static Map<Integer, Integer> consumptionNetMap;

    //初始的MST
    private static Weight[][] initMST;


    //满足的服务器的列表
    private static ArrayList<Integer> severList;
    //满足的消费点的列表
    private static ArrayList<Integer> satisfiedComsumptionNode;

    //表示不可达的常量
    private static Weight NO_PATH_WEIGHT = new Weight();

    static {
        NO_PATH_WEIGHT.setTotalBandwidth(0);
        NO_PATH_WEIGHT.setNetRentCost(101);
    }


    public static String[] deployServer(String[] graphContent) {
        /**do your work here**/
        initData(graphContent);
        return solveQuestion();
    }

    /**
     * 1. 根据邻接矩阵，生成一个最小生成树
     *
     * @return 输出结果
     * @see Deploy#createMST(int)
     * 2. 对最小生成树的节点，按照度的大小进行排序
     * @see Deploy#sortMSTNode(Map)
     * 3. 在排序好的数组中，选择度最大的点，，也就是下标0的点，暂定为服务器
     * 4. 判断该服务器，是否至少满足一个消费节点。
     * @see Deploy#isSatisfyConsumptionNode(int, int[], int[])
     * 5. 如果满足，则判断所有的消费节点是否全部满足，如果都满足。则return 输出，如果不能全部满足，则生成残图，并跳转到第一步，再次执行
     * @see Deploy#generateRemainGraph()
     * 6. 如果不满足，则跳到第三步，下标为1的点，暂定为服务器，依次类推。
     */

    public static String[] solveQuestion() {
        while (!isSatisfyAllConsumptionNodes()) {
            //从不满足消费节点相邻的网络节点创建生成树
            int start = 0;
            for (int i = 0; i < consumptionNodeCount; i++) {
                if (!satisfiedComsumptionNode.contains(i)) {
                    start = consumptionInfo.get(i).getLinkedID();
                }
            }
            Map<Integer, ArrayList<Integer>> mst = createMST(start);
//            System.out.println("创建了生成树----------------------------------开始迭代");
            int[] node = sortMSTNode(mst);
            for (int i = 0; i < node.length; i++) {
                if (severList.contains(node[i])) {
//                    continue;
                }
                int serverID = node[i];
                int[] orders = doBFSInMST(serverID);
                //与消费节点相连的网络节点
                int[] netNodes = new int[consumptionNodeCount];
                int count = 0;
                //层次遍历生成树，标记出是消费节点相连的网络节点的位置。
                for (int k = 0; k < orderInBFS.length; k++) {
                    for (int j = 0; j < consumptionNodeCount; j++) {
                        if (orderInBFS[k] == consumptionInfo.get(j).getLinkedID()) {
                            netNodes[count++] = orderInBFS[k];
                            break;
                        }
                    }
                }
                if (isSatisfyConsumptionNode(serverID, netNodes, orders)) {
                    break;
                }

            }
            generateRemainGraph();
        }

        doOptimization();
        String[] result = new String[2 + paths.size()];
        result[0] = paths.size() + "";
        result[1] = "";
        for (int i = 0; i < paths.size(); i++) {
            StringBuffer sb = new StringBuffer();
            LinkedList<Integer> path = paths.get(i);
            for (int j = path.size() - 1; j >= 0; j--) {
                if (j != 0) {
                    sb.append(path.get(j) + " ");
                } else {
                    sb.append(path.get(j));
                }
            }
            result[i + 2] = sb.toString();
        }
        return result;
    }

    public static List<LinkedList<Integer>> cloneLinkList(List<LinkedList<Integer>> paths) {
        List<LinkedList<Integer>> linkedLists = new ArrayList<LinkedList<Integer>>();

        for (int i = 0; i < paths.size(); i++) {
            LinkedList<Integer> ll = new LinkedList<Integer>();
            LinkedList<Integer> l2 = paths.get(i);

            for (int j = 0; j < l2.size(); j++) {
                ll.add(l2.get(j));
            }
            linkedLists.add(ll);
        }

        return linkedLists;
    }

    /**
     * 对选路出的结果进行优化,出现的问题是合并完，还有可以合并的结果。
     */
    private static void doOptimization() {
        List<LinkedList<Integer>> tmp = cloneLinkList(paths);

        for (int g = 0; g < tmp.size(); g++) {
            LinkedList<Integer> item = tmp.get(g);
            for (int i = item.size() - 1; i > 1; i--) {
                if (severList.contains(item.get(i))) {
                    for (int j = item.size() - 1; j > i; j--) {
                        item.remove(j);
                    }
                }
            }
        }

        paths = tmp;
        int[] equals;
        while ((equals = isExistCombinePath(paths)).length != 0) {
            int value1 = paths.get(equals[0]).get(0);
            int value2 = paths.get(equals[1]).get(0);
            paths.get(equals[1]).set(0, value1 + value2);
            paths.remove(equals[0]);
        }

    }


    /**
     * 判断一个路经集合是否存在两条可以合并的路线
     * 合并的原则是：除去所需带宽之外的，元素是全部相等，或者存在包含的情况。
     *
     * @param paths
     * @return
     */
    public static int[] isExistCombinePath(List<LinkedList<Integer>> paths) {
        List<LinkedList<Integer>> tmp = cloneLinkList(paths);
        for (int i = 0; i < tmp.size(); i++) {
            tmp.get(i).remove(0);
        }
        for (int i = 0; i < tmp.size(); i++) {
            for (int j = i + 1; j < tmp.size(); j++) {
                if (isEqualOfTwoList(tmp.get(i), tmp.get(j))) {
                    return new int[]{i, j};
                    //containsAll可能包含顺序不正确的包含，比如1，2，3，5 包含 5 3 2 这种
                } else if (tmp.get(i).containsAll(tmp.get(j)) || tmp.get(j).containsAll(tmp.get(i))) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{};

    }


    public static boolean isEqualOfTwoList(List<Integer> l1, List<Integer> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }

        for (int i = 1; i < l1.size(); i++) {
            if (!l1.get(i).equals(l2.get(i))) {
                return false;
            }
        }

        return true;
    }


    /**
     * @param beginNode 在生成树中进行BFS时的起始节点
     * @return 数组，该数组下标是网络节点的序号，数组的值是该网络节点层次遍历时的前驱节点
     */
    public static int[] doBFSInMST(int beginNode) {
        int count = 0;
        orderInBFS = new int[netNodeCount];
        int[] orders = new int[netNodeCount];
        for (int i = 0; i < netNodeCount; i++) {
            orders[i] = -1;
        }

        boolean[] visited = new boolean[netNodeCount];
        for (int i = 0; i < netNodeCount; i++) {
            visited[i] = false;
        }

        Queue<Integer> queue = new LinkedList<Integer>();
        queue.offer(beginNode);
        orderInBFS[count++] = beginNode;
        visited[beginNode] = true;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int i = 0; i < netNodeCount; i++) {
                if (current == i) {
                    continue;
                }
                if (isLinked(MSTgraph, current, i)) {
                    if (!visited[i]) {
                        queue.offer(i);
                        orderInBFS[count++] = i;
                        orders[i] = current;
                        visited[i] = true;
                    }
                }
            }
        }
        return orders;
    }


    /**
     * 对生成树的节点进行排序，排序的原则是按照度从大到小
     *
     * @param mst 一个生成树
     * @return 返回排序后的生成树的节点数组
     */
    public static int[] sortMSTNode(Map<Integer, ArrayList<Integer>> mst) {
        List<Map.Entry<Integer, ArrayList<Integer>>> list = new ArrayList<Map.Entry<Integer, ArrayList<Integer>>>(
                mst.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, ArrayList<Integer>>>() {
            public int compare(Map.Entry<Integer, ArrayList<Integer>> o1, Map.Entry<Integer, ArrayList<Integer>> o2) {
                return (o2.getValue().size() - o1.getValue().size());
            }
        });
        int i = 0;
        int[] keySortedByDegree = new int[mst.size()];
        for (Map.Entry<Integer, ArrayList<Integer>> newEntry : list) {
            keySortedByDegree[i++] = newEntry.getKey();
        }
        // ArrayList中元素是有序的
        return keySortedByDegree;
    }

    /**
     * 1. 按照生成树去判断服务器能不能走到消费节点
     * 2. 如果全部能走到，则返回true
     *
     * @param serverID 服务器的ID
     * @param netNodes 消费节点相邻的网络节点
     * @param orders   在生成树中点的前驱节点
     * @return true 如果部署的服务器至少满足一个的消费节点。false 不满足
     */
    public static boolean isSatisfyConsumptionNode(int serverID, int[] netNodes, int[] orders) {

        boolean isSatisfy = false;

        //保存状态，以便回滚
        Weight[][] tmpGraph = cloneGraph(MSTgraph);
        List<LinkedList<Integer>> tmpPaths = new ArrayList<LinkedList<Integer>>(netNodes.length);
        Map<Integer, ConsumptionNodeInfo> tmpInfo = cloneConsumptionInfo(consumptionInfo);

        for (int i = 0; i < netNodes.length; i++) {
//            System.out.println("当前的消费节点是" + consumptionNetMap.get(netNodes[i]));
            //得到消费节点到服务节点的一个路径，若满足，则minBandWidth = 消费节点需求的带宽。若不满足，则minBandWidth = 该路径最短的带宽
            int cursor = netNodes[i];
            int requiredBandWidth = consumptionInfo.get(consumptionNetMap.get(netNodes[i])).getRequiredBandWidth();
            int minBandWidth = 101;
            Queue<Integer> successQueue = new LinkedList<Integer>();

            if (!satisfiedComsumptionNode.contains(consumptionNetMap.get(netNodes[i]))) {
                successQueue.offer(cursor);
            } else {
                continue;
            }
            while (cursor != serverID && !satisfiedComsumptionNode.contains(consumptionNetMap.get(netNodes[i]))) {

                //生成树矩阵
                int totalBandWidth = MSTgraph[cursor][orders[cursor]].getTotalBandwidth();
                int usedBandWidth = MSTgraph[cursor][orders[cursor]].getUsedBandWidth();

                if (minBandWidth > (totalBandWidth - usedBandWidth) && totalBandWidth > usedBandWidth) {
                    minBandWidth = totalBandWidth - usedBandWidth;
                }
//                System.out.println("cursor " + cursor + " orders[cursor] " + orders[cursor] + "                   total   " + MSTgraph[cursor][orders[cursor]].getTotalBandwidth()
//                        + " used " + MSTgraph[cursor][orders[cursor]].getUsedBandWidth());
                if (totalBandWidth <= usedBandWidth) {
                    successQueue = null;
                    break;
                }
                successQueue.offer(orders[cursor]);
                cursor = orders[cursor];
            }
            if (successQueue != null) {
                LinkedList<Integer> path = new LinkedList<Integer>();
                if (minBandWidth >= requiredBandWidth) {
                    isSatisfy = true;
                    satisfiedComsumptionNode.add(consumptionNetMap.get(netNodes[i]));
                    severList.add(serverID);
                    minBandWidth = requiredBandWidth;
                }
                //设置消费节点还需带宽
                ConsumptionNodeInfo info = consumptionInfo.get(consumptionNetMap.get(netNodes[i]));
                consumptionInfo.get(consumptionNetMap.get(netNodes[i])).setRequiredBandWidth(info.getRequiredBandWidth() - minBandWidth);


                //设置生成树邻接矩阵上的usedBandWidth是minBandWidth
                //将队列中的元素，依次出队，放入放入栈中，就是最终的路线

                path.add(minBandWidth);
                path.add(consumptionNetMap.get(netNodes[i]));
                int start, end;
                start = successQueue.poll();
                path.add(start);
                while (successQueue.size() != 0) {
                    end = successQueue.poll();
                    path.add(end);
//                    if (MSTgraph[start][end].getUsedBandWidth() < MSTgraph[start][end].totalBandwidth) {
                    MSTgraph[start][end].setUsedBandWidth(minBandWidth + MSTgraph[start][end].getUsedBandWidth());
//                    System.out.println("MSTGraph start " + start + " end " + end + "                       used " + MSTgraph[start][end].getUsedBandWidth() + " total " + MSTgraph[start][end].getTotalBandwidth());
                    MSTgraph[end][start].setUsedBandWidth(minBandWidth + MSTgraph[end][start].getUsedBandWidth());
//                    }
                    start = end;
                }
                tmpPaths.add(path);
//                System.out.println("结束一条路径，服务器是 " + serverID + "————————————————————————————————————————————");
            } else {
//                System.out.println("没有选择该路径-------------------------------------------------------------------");
            }
        }

        if (isSatisfy) {
            paths.addAll(tmpPaths);
//            System.out.println("结束一个服务器————————————————————————————————————————————");

        } else {
            MSTgraph = cloneGraph(tmpGraph);
            consumptionInfo = cloneConsumptionInfo(tmpInfo);
            tmpPaths = null;
        }

        return isSatisfy;

    }

    private static Map<Integer, ConsumptionNodeInfo> cloneConsumptionInfo(Map<Integer, ConsumptionNodeInfo> consumptionInfo) {

        Map<Integer, ConsumptionNodeInfo> infoMap = new HashMap<Integer, ConsumptionNodeInfo>();

        for (int i = 0; i < consumptionInfo.size(); i++) {
            ConsumptionNodeInfo consumptionNodeInfo = new ConsumptionNodeInfo();
            ConsumptionNodeInfo c2 = consumptionInfo.get(i);
            consumptionNodeInfo.setLinkedID(c2.getLinkedID());
            consumptionNodeInfo.setRequiredBandWidth(c2.getRequiredBandWidth());
            infoMap.put(i, consumptionNodeInfo);
        }

        return infoMap;

    }


    /**
     * 对graph进行克隆
     *
     * @param graph 图的邻接矩阵
     * @return 克隆的图的邻接矩阵
     */
    public static Weight[][] cloneGraph(Weight[][] graph) {
        Weight[][] newGraph = new Weight[graph.length][graph.length];
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph.length; j++) {
                newGraph[i][j] = cloneWeight(graph[i][j]);
            }
        }

        return newGraph;
    }

    public static Weight cloneWeight(Weight weight) {
        Weight newWeight = new Weight();
        newWeight.setUsedBandWidth(weight.getUsedBandWidth());
        newWeight.setTotalBandwidth(weight.getTotalBandwidth());
        newWeight.setNetRentCost(weight.getNetRentCost());
        return newWeight;
    }


    public static boolean isSatisfyAllConsumptionNodes() {
        return satisfiedComsumptionNode.size() == consumptionNodeCount;
    }

    public static void generateRemainGraph() {
        for (int i = 0; i < netNodeCount; i++) {
            for (int j = 0; j < netNodeCount; j++) {
                if (isLinked(MSTgraph, i, j)) {
                    graph[i][j].setUsedBandWidth(MSTgraph[i][j].getUsedBandWidth());
                }
            }
        }

        MSTgraph = cloneGraph(initMST);

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
        MSTgraph = new Weight[netNodeCount][netNodeCount];
        orderInBFS = new int[netNodeCount];
        paths = new LinkedList<LinkedList<Integer>>();
        satisfiedComsumptionNode = new ArrayList<Integer>(consumptionNodeCount);
        severList = new ArrayList<Integer>(netNodeCount);


        //初始化图中的权值信息，默认为所有节点不可达
        for (int i = 0; i < netNodeCount; i++) {
            for (int j = 0; j < netNodeCount; j++) {
                graph[i][j] = cloneWeight(NO_PATH_WEIGHT);
            }
        }
        MSTgraph = cloneGraph(graph);
        initMST = cloneGraph(MSTgraph);

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
        consumptionInfo = new HashMap<Integer, ConsumptionNodeInfo>(consumptionNodeCount);
        consumptionNetMap = new HashMap<Integer, Integer>(consumptionNodeCount);

        while (count-- > 0) {
            String[] infos = lineTos(graphContent[lineNum++]);
            int consumptionID = sToi(infos[0]);
            int linkedID = sToi(infos[1]);
            int requiredBandWidth = sToi(infos[2]);

            ConsumptionNodeInfo info = new ConsumptionNodeInfo();
            info.setLinkedID(linkedID);
            info.setRequiredBandWidth(requiredBandWidth);
            consumptionInfo.put(consumptionID, info);
            consumptionNetMap.put(linkedID, consumptionID);
        }

    }

    /**
     * 给定一幅无向图的邻接矩阵，和矩阵的2个下标，判断该2点是否相连
     *
     * @param graph
     * @param start
     * @param end
     * @return
     */
    public static boolean isLinked(Weight[][] graph, int start, int end) {
        if (start == end) {
            return false;
        }
        if (graph[start][end].getNetRentCost() < NO_PATH_WEIGHT.getNetRentCost() &&
                graph[start][end].getTotalBandwidth() > NO_PATH_WEIGHT.getTotalBandwidth() &&
                graph[end][start].getNetRentCost() < NO_PATH_WEIGHT.getNetRentCost() &&
                graph[end][start].getTotalBandwidth() > NO_PATH_WEIGHT.getTotalBandwidth()) {
            return true;
        } else {
            return false;
        }
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
        //保存被并入生成树的节点的前驱节点（邻接点）
        int[] preset = new int[netNodeCount];

        //
        Map<Integer, ArrayList<Integer>> mst = new HashMap<Integer, ArrayList<Integer>>(netNodeCount);

        //初始化
        for (int i = 0; i < netNodeCount; i++) {
            lowcost[i] = cloneWeight(graph[start][i]);
            vset[i] = 0;
            preset[i] = start;
            ArrayList<Integer> a = new ArrayList<Integer>();
            mst.put(i, a);
        }
        //起始节点被并入生成树
        vset[start] = 1;
        for (int i = 1; i < netNodeCount; i++) {
            min = cloneWeight(NO_PATH_WEIGHT);
            //选出候选边中的最小者
            for (int j = 0; j < netNodeCount; j++)
                if (vset[j] == 0 && compareWeight(lowcost[j], min)) {
                    min = cloneWeight(lowcost[j]);
                    k = j;
                }
            //k并入生成树
            vset[k] = 1;
            //k设为中介节点
            v = k;
            //把边k和其前驱节点连接的边保存到生成树
            MSTgraph[preset[k]][k] = cloneWeight(min);
            MSTgraph[k][preset[k]] = cloneWeight(min);
            mst.get(preset[v]).add(v);
            //以刚并入的顶点v为中介，更新候选边和某些节点的前驱节点
            for (int l = 0; l < netNodeCount; l++) {
                if (vset[l] == 0 && compareWeight(graph[v][l], lowcost[l])) {
                    lowcost[l] = cloneWeight(graph[v][l]);
                    preset[l] = v;
                }
            }
        }
        return mst;
    }

    /**
     * 比较两个weight的大小，优先比较网络租用费。网络租用费低，则weight小。
     * 如果网络租用费一致，则比较总带宽，总带宽大的，则weight小
     *
     * @return 前者比后者小，返回TRUE，否则返回FALSE
     * @param两个weight
     */
    public static boolean compareWeight(Weight weight1, Weight weight2) {

        if (weight1.getNetRentCost() < weight2.getNetRentCost()) return true;
        else if (weight1.getNetRentCost() > weight2.getNetRentCost()) return false;
        else {
            if (weight1.getTotalBandwidth() - weight1.getUsedBandWidth() > weight2.getTotalBandwidth() - weight2.getUsedBandWidth())
                return true;
        }

        return false;
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


    public static void printMST(int start) {
        Map<Integer, ArrayList<Integer>> map = createMST(start);
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ArrayList<Integer> lists = (ArrayList<Integer>) entry.getValue();
            for (int i = 0; i < lists.size(); i++) {
//                System.out.println(entry.getKey() + " " + lists.get(i));
            }
        }
    }

}

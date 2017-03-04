# coding=utf-8
# !/usr/bin/python

import networkx as nx
import matplotlib.pyplot as plt

file_name = "case"


def draw():
    G = nx.Graph()
    with open(file_name, "r") as f:
        while 1:
            line = f.readline()
            if not line:
                break

            list_node = line.split(" ")
            start = list_node[0]
            end = list_node[1]
            weight = list_node[2]
            G.add_edge(start,end,weight=weight)



    pos = nx.spectral_layout(G)


    nx.draw_networkx(G,with_labels = True, alpha=0.3)

    # nx.draw_networkx(G)
    plt.savefig("pic.png")
    plt.show()

        # G = nx.complete_graph(8)
        # G.add_edge(1, 2)
        # nx.draw_networkx(G)
        #
        # plt.show()


draw()

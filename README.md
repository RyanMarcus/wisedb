# WiSeDB

This project contains code for the WiSeDB project, a part of the [XCloud project at Brandeis University](http://www.cs.brandeis.edu/~olga/XCloud.html). This is a Java implemention of the WiSeDB system as described in:


Marcus, Ryan, and Olga Papaemmanouil. "[Workload Management for Cloud Databases via Machine Learning.](http://www.cs.brandeis.edu/~olga/publications/clouddm-2016.pdf)" Workshop on Cloud Data Management and the IEEE International Conference on Data Engineering, CloudDM. Vol. 16. (2016)

Marcus, Ryan, and Olga Papaemmanouil. "[WiSeDB: A Learning-based Workload Management Advisor for Cloud Databases.](http://arxiv.org/abs/1601.08221)" arXiv preprint arXiv:1601.08221 (technical report, 2016).





## What's it do?

WiSeDB is designed to assist cloud applications in making three types of decisions:

1. Resource provisioning: determining the number of VMs needed to process a workload
1. Query placement: determining which query should execute on which VM
1. Query scheduling: scheduling the queries within a VM

WiSeDB provides a complete, integrated answer to these problems, and it does so in an *SLA-aware* way.

![Animation of three tasks](https://raw.githubusercontent.com/RyanMarcus/wisedb/master/res/workloadmang.gif?raw=true)

## Example

Imagine you have three types of tasks:

* Task `A`, which takes 2 minutes
* Task `B`, which takes 3 minutes
* Task `C`, which takes 4 minutes

Imagine you have a workload with 2 `A`s, 2 `B`s, and `2` Cs. Your goal (SLA) is to complete all the tasks within 9 minutes. Since you want to minimize the number of VMs you have to rent (to reduce costs), you want to find the fewest number of VMs that can process the workload on time. In this case, the workload management problem is equivalent to [the bin packing problem](https://en.wikipedia.org/wiki/Bin_packing_problem) and is thus `NP-Hard`.

One possible solution, using the traditional `FFD` heuristic, is:

* VM1: `[C, C]` (8 minutes)
* VM2: `[C, B]` (7 minutes)
* VM3: `[B, B, A]` (8 minutes) 
* VM4: `[A, A, A]` (6 minutes)

... which uses four virtual machines. However, the solution we really want is:

* VM1: `[C, B, A]` (9 minutes)
* VM2: `[C, B, A]` (9 minutes)

... which uses only two virtual machines.

WiSeDB is a system that uses machine learning in order to generate *customized heuristics* that are tailored to your application's specific workload and performance goals. When given this workload specification and workload goal, WiSeDB learns a strategy of "place an instance of C, then B, then A, then create a new VM and repeat", which produces the desired outcome.

Of course, WiSeDB does not always find an optimal solution, but in our experiments we find that it always outperforms standard heuristics.

More complicated scenarios can emerge when the SLA changes, for example, to have different deadlines for different query types, to restrict the *average* performance of a query, or to enforce a percentile-based constraint (e.g. 90% of the queries must complete within 10 minutes).

![example of per query SLA](https://raw.githubusercontent.com/RyanMarcus/wisedb/master/res/sla.png?raw=true)



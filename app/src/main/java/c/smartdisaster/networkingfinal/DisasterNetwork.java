package c.smartdisaster.networkingfinal;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;

public class DisasterNetwork {

    // General variables //
    boolean paused; // if we have paused passage of time
    double elapsedTime; // time we have spent unpaused //
    int jobId; // the id of the latest generated job
    int channelId; // the id of the latest generate channel //
    int jobsPerSecond; // how many jobs are generated per second
    int numAvailableChannels; // the number of active channels
    ArrayList<Job> allJobs; // the list of all jobs, created //
    ArrayList<Channel> allChannels; // the list of all channels //
    ArrayList<Job> jobsTransferring;
    ArrayList<Job> activeJobs;
    // Compute Variables
    public ArrayList<Job> completedJobs;
    CPU localCenter;
    RemoteCenter remoteCenter;

    // Network Variables //
    public PriorityQueue<Job> jobPool; // job pool for network transfers | sorted by priority
    public PriorityQueue<Channel> channelPool; // channelPool for network transfers | sorted by bandwidth
    public ArrayList<Device> deviceList;

    DisasterNetwork () {
        jobId = 1;
        channelId = 1;
        numAvailableChannels = 3;
        jobsPerSecond = 1;
        paused = false;
        allChannels = new ArrayList<Channel>();
        activeJobs = new ArrayList<Job>();
        allJobs = new ArrayList<Job>();
        completedJobs = new ArrayList<Job>();
        jobPool = new PriorityQueue<Job>(5, new JobComparator());
        channelPool = new PriorityQueue<Channel>(5, new ChannelComparator());
        localCenter = new CPU("Local Center", 10, 5);
        remoteCenter = new RemoteCenter();
        localCenter.network = this;
        remoteCenter.network = this;
        deviceList = new ArrayList<Device>();
        jobsTransferring = new ArrayList<Job>();
    }

    // Creates a channel with a random bandwidth between 1 and 10 //
    void CreateChannel () { // on click event for channel dial
        numAvailableChannels++;
        int bandwidth = new Random().nextInt(20) + 1;
        Channel c = new Channel(bandwidth, channelId++);
        channelPool.offer(c);
    }

    // Destroys a channel | On click event for channel dial //
    void DestroyChannel () {
        if (channelPool.size() != 0) { // if we can destroy an unallocated channel
            channelPool.poll(); // do it
            return;
        }
        if (jobsTransferring.size() == 0) // if there are no allocated channels
            return; // do nothing
        // Otherwise find a random transferring job and interrupt it by destroying its channel //
        Random rand = new Random();
        int r = rand.nextInt(100);
        Job j = jobsTransferring.get(r % jobsTransferring.size());
        j.channel = null;
        j.state = "Interrupted";
        jobsTransferring.remove(j);
        numAvailableChannels--;
    }

    // generates jobs every second | Called in Workflow.java
    void GenerateJobs () {
        for (int i = 0; i < jobsPerSecond; i++) {
            Random rand = new Random();
            int deviceID = rand.nextInt(100) % deviceList.size();
            Job j = deviceList.get(deviceID).CreateJob(jobId++);
            j.location = "Job Queue";
            allJobs.add(j);
            activeJobs.add(j);
            jobPool.offer(j);
        }
    }

    // pulls jobs and channels from respective pools and begins transfer //
    void LoadJobs () {
        while (!jobPool.isEmpty() && !channelPool.isEmpty()) {
            Job j = jobPool.poll();
            Channel c = channelPool.poll();
            j.channel = c;
            j.state = "Transferring";
            j.location = "Channel" + c.id;
            jobsTransferring.add(j);
        }
    }

    void TransferJobs () { // Transfers jobs from devices to local center
        for (int i = 0; i < jobsTransferring.size(); i++) {
            Job j = jobsTransferring.get(i);
            if (j.channel == null)
                continue;
            j.progress = Math.min(j.progress + j.channel.bandwidth, j.totalPayLoad);
            if (j.progress >= j.totalPayLoad) {
                j.progress = 0;
                jobsTransferring.remove(j);
                if (channelPool.size() < numAvailableChannels) channelPool.add(j.channel);
                j.channel = null;
                localCenter.AddJob(j);
            }
        }
    }


    void IncrementTime (int interval) {
        IncrementQueueTime(interval);
        IncrementCPUTime(interval);
        IncrementTransferTime(interval);
    }

    void IncrementQueueTime (int interval) {
        if (jobPool.size() == 0)
            return;
        ArrayList<Job> jobs = new ArrayList<Job>();
        while (!jobPool.isEmpty()) {
            Job j = jobPool.poll();
            j.time += interval;
            j.networkTime += interval;
            jobs.add(j);
        }
        for (int i = 0; i < jobs.size(); i++)
            jobPool.offer(jobs.get(i));
    }

    void IncrementCPUTime (int interval) {
        for (int i = 0; i < localCenter.jobList.size(); i++) {
            localCenter.jobList.get(i).time += interval;
            localCenter.jobList.get(i).computeTime += interval;
        }
        for (int i = 0; i < remoteCenter.jobList.size(); i++)
            remoteCenter.jobList.get(i).computeTime += interval;

    }

    void IncrementTransferTime (int interval) {
        for (int i = 0; i < jobsTransferring.size(); i++) {
            jobsTransferring.get(i).time += interval;
            jobsTransferring.get(i).networkTime += interval;
        }
        for (int i = 0; i < localCenter.transferringJobs.size(); i++) {
            localCenter.transferringJobs.get(i).time += interval;
            localCenter.transferringJobs.get(i).networkTime += interval;
        }

    }

    void CreateDevices () {
        for (int i = 1; i <= 3; i++) {
            deviceList.add(new Device(i, i));
        }
    }
    void CreateChannels () {
        Channel c1 = new Channel(10, channelId++);
        Channel c2 = new Channel(5, channelId++);
        Channel c3 = new Channel(2, channelId++);
        channelPool.offer(c1);
        channelPool.offer(c2);
        channelPool.offer(c3);
        allChannels.add(c1);
        allChannels.add(c2);
        allChannels.add(c3);
    }

    void PrintTransferring () {
        for (int i = 0; i < jobsTransferring.size(); i++)
            System.out.println(jobsTransferring.get(i).GetProgress());
        System.out.println();
    }

    void PrintComputing () {
        for (int i = 0; i < localCenter.jobList.size(); i++)
            System.out.println(localCenter.jobList.get(i).GetProgress());
        System.out.println();
    }

    void PrintRemoteTransfer() {
        for (int i = 0; i < localCenter.transferringJobs.size(); i++)
            System.out.println(localCenter.transferringJobs.get(i).GetProgress());
        System.out.println();
    }

    void PrintRemoteCompute () {
        for (int i = 0; i < remoteCenter.jobList.size(); i++)
            System.out.println(remoteCenter.jobList.get(i).GetProgress());
        System.out.println();
    }

    void PrintJobs () {
        if (jobPool.size() == 0)
            return;
        ArrayList<Job> jobs = new ArrayList<Job>();
        while (!jobPool.isEmpty()) {
            jobs.add(jobPool.poll());
            System.out.print(jobs.get(jobs.size()-1).priority);
        }
        for (int i = 0; i < jobs.size(); i++)
            jobPool.offer(jobs.get(i));
    }

    void PrintChannels () {
        if (channelPool.size() == 0)
            return;
        ArrayList<Channel> channels = new ArrayList<Channel>();
        while (!channelPool.isEmpty()) {
            channels.add(channelPool.poll());
            System.out.print(channels.get(channels.size()-1).bandwidth);
        }
        for (int i = 0; i < channels.size(); i++)
            channelPool.offer(channels.get(i));
    }

}

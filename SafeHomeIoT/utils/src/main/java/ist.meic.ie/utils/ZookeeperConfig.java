package ist.meic.ie.utils;

import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;

public class ZookeeperConfig {
    private ZkClient zkClient;

    private String zookeeperConnect;
    private int sessionTimeoutMs;
    private int connectionTimeoutMs;

    public ZookeeperConfig(String zookeeperConnect, int sessionTimeoutMs, int connectionTimeoutMs) {
        this.zookeeperConnect = zookeeperConnect;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
    
    public ZkClient openZkClient() {
        this.zkClient = new ZkClient(this.zookeeperConnect, this.sessionTimeoutMs, this.connectionTimeoutMs, ZKStringSerializer$.MODULE$);
        return this.zkClient;
    }

    public ZkConnection connect() {
        return new ZkConnection(zookeeperConnect);
    }

    public void closeZkClient() {
        this.zkClient.close();
    }

    public ZkClient getZkClient() {
        return zkClient;
    }
}

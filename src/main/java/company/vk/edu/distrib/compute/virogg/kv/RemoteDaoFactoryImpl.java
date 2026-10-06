package company.vk.edu.distrib.compute.virogg.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

@RemoteDaoFactoryTest
public final class RemoteDaoFactoryImpl implements RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) {
        if (ports.length != 1) {
            throw new IllegalArgumentException("Exactly one KV service port is required");
        }
        return new RemoteStringDao(ports[0]);
    }
}

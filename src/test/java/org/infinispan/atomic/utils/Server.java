package org.infinispan.atomic.utils;

import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;

/**
 * @author Pierre Sutra
 */
public class Server implements Runnable {

   private static final String defaultServer ="localhost:11222";
   private boolean USE_TRANSACTIONS = false;
   private CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
      
   @Option(name = "-server", required = true, usage = "ip:port or ip of the server")
   private String server = defaultServer;
   
   @Option(name = "-proxy", usage = "proxy server as seen by clients")
   private String proxyServer = null;

   @Option(name = "-rf", usage = "replication factor")
   private int replicationFactor = 1;

   @Option(name = "-p", usage = "use persistence via a single file data store (emptied at each start)")
   private boolean usePersistency = false;

   public Server () {}
   
   public static void main(String args[]) {
      new Server().doMain(args);
   }
   
   
   public void doMain(String[] args) {
      
      CmdLineParser parser = new CmdLineParser(this);

      parser.setUsageWidth(80);

      try {
         if(args.length<2)
            throw new CmdLineException(parser, "Not enough arguments are given.");
         parser.parseArgument(args);
      } catch( CmdLineException e ) {
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
         System.err.println();
         return;
      }

      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute(this);
      try {
         executor.shutdown();
      }catch (Exception e){
         // ignore
      }
      
   }

   @Override 
   public void run() {

      String host = server.split(":")[0];
      int port = Integer.valueOf(
            server.split(":").length == 2
                  ? server.split(":")[1] : "11222");

      GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      gbuilder.transport().clusterName("aof-cluster");
      gbuilder.transport().nodeName("aof-server-"+host);

      ConfigurationBuilder builder= getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      builder
            .clustering()
            .hash()
            .numOwners(replicationFactor)
            .compatibility()
            .enable();
      builder.locking()
            .concurrencyLevel(10000)
            .useLockStriping(false);

      if (usePersistency)
         builder.persistence()
               .addSingleFileStore()
               .location(System.getProperty("."))
               .purgeOnStartup(true)
               .create();

      EmbeddedCacheManager cm = new DefaultCacheManager(gbuilder.build(), builder.build(), true);
      
      HotRodServerConfigurationBuilder hbuilder = new HotRodServerConfigurationBuilder();
      hbuilder.topologyStateTransfer(true);
      hbuilder.host(host);
      hbuilder.port(port);

      if (proxyServer != null && !proxyServer.equals(defaultServer)) {
         String proxyHost = proxyServer.split(":")[0];
         int proxyPort = Integer.valueOf(
               proxyServer.split(":").length == 2
                     ? proxyServer.split(":")[1] : "11222");
         hbuilder.proxyHost(proxyHost);
         hbuilder.proxyPort(proxyPort);
      }

      hbuilder.workerThreads(100);
      hbuilder.tcpNoDelay(true);

      HotRodServer server = new HotRodServer();
      server.start(hbuilder.build(),cm);
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, new FilterConverterFactory());
      
      System.out.println("LAUNCHED");
      
      try {
         synchronized (this) {
            this.wait();
         }
      } catch (InterruptedException e) {
         // ignore. 
      }
      
   }
}
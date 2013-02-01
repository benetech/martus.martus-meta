package org.martus.loadtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.MartusLogger;
import org.martus.common.MartusUtilities;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPIWithHelpers;
import org.martus.common.packet.UniversalId;
import org.martus.util.DirectoryUtils;
import org.martus.util.StreamableBase64;

/**
 * @author roms
 *         Date: 1/30/13
 */
public class ServerLoader {

    public ServerLoader(String serverIP, String magicWord, int numThreads, int numBulletins)
    {
        this.serverIP = serverIP;
        this.magicWord = magicWord;
        this.numThreads = numThreads;
        this.numBulletins = numBulletins;
    }

    public final static void main( String[] args )
    {
        if( args.length < 4 )
        {
            usage("Not enough arguments.");
        }
        else {
            String serverIp = args[0];
            String magicWord = args[1];
            int numThreads = Integer.valueOf(args[2]);
            int numBulletins = Integer.valueOf(args[3]);

            final ServerLoader loader = new ServerLoader(serverIp, magicWord, numThreads, numBulletins);
            loader.startLoading();

        }
    }

    public void startLoading()
    {

        try
        {
            martusCrypto = new MartusSecurity();
            martusCrypto.createKeyPair();

            NonSSLNetworkAPIWithHelpers server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);
            String result = server.getServerPublicKey(martusCrypto);

            gateway = ClientSideNetworkGateway.buildGateway(serverIP, result);
            NetworkResponse response = gateway.getUploadRights(martusCrypto, magicWord);
            if (!response.getResultCode().equals(NetworkInterfaceConstants.OK))
            {
                MartusLogger.log("couldn't verify magic word");
                return;
            }

            //can now create bulletins;
        } catch (Exception e) {
            MartusLogger.log("unable to verify server info");
            MartusLogger.logException(e);
        }

        store = new ClientBulletinStore(martusCrypto);
        try
        {
            tempDir = DirectoryUtils.createTempDir();
            store.doAfterSigninInitialization(tempDir);
            store.createFieldSpecCacheFromDatabase();
        } catch (Exception e) {
            MartusLogger.log("unable to initialize store");
            MartusLogger.logException(e);
        }

        zippedBulletins = new File[numBulletins];
        bulletinIds = new UniversalId[numBulletins];
        try
        {
            createZippedBulletins();

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            int i = 0;
            for (File file : zippedBulletins) {
                Runnable sender = new BulletinSenderRunnable(martusCrypto, gateway, file, bulletinIds[i++]);
                executor.execute(sender);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                //do nothing - just waiting
            }
            MartusLogger.log("Finished sending bulletins");
        } catch (Exception e) {
            MartusLogger.log("problem sending bulletins");
            MartusLogger.logException(e);
        }
        DirectoryUtils.deleteEntireDirectoryTree(tempDir);
    }

    private void createZippedBulletins() throws Exception
    {
        for (int i = 0; i < numBulletins; i++) {
            Bulletin bulletin = createBulletin(i);
            store.saveBulletin(bulletin);
            File file = File.createTempFile("tmp_send_", ".zip", tempDir);
            BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(store.getDatabase(),
                    bulletin.getDatabaseKey(), file, bulletin.getSignatureGenerator());
            zippedBulletins[i] = file;
            bulletinIds[i] = bulletin.getUniversalId();
            store.destroyBulletin(bulletin);
        }
    }

    private Bulletin createBulletin(int num) throws Exception
    {
        Bulletin b = store.createEmptyBulletin();
        b.set(Bulletin.TAGTITLE, "loadtest title " + num);
        b.set(Bulletin.TAGSUMMARY, "loadtest summary " + num);
        b.setDraft();
        b.setAllPrivate(true);
        return b;
    }

    public static String uploadBulletinZipFile(UniversalId uid, File tempFile, ClientSideNetworkGateway gateway,
            MartusCrypto crypto)
            throws MartusUtilities.FileTooLargeException, IOException, MartusCrypto.MartusSignatureException
    {
        final int totalSize = MartusUtilities.getCappedFileLength(tempFile);
        int offset = 0;
        byte[] rawBytes = new byte[NetworkInterfaceConstants.CLIENT_MAX_CHUNK_SIZE];
        FileInputStream inputStream = new FileInputStream(tempFile);
        String result = null;
        while(true)
        {
            int chunkSize = inputStream.read(rawBytes);
            if(chunkSize <= 0)
                break;
            byte[] chunkBytes = new byte[chunkSize];
            System.arraycopy(rawBytes, 0, chunkBytes, 0, chunkSize);

            String authorId = uid.getAccountId();
            String bulletinLocalId = uid.getLocalId();
            String encoded = StreamableBase64.encode(chunkBytes);

            NetworkResponse response = gateway.putBulletinChunk(crypto,
                                authorId, bulletinLocalId, totalSize, offset, chunkSize, encoded);
            result = response.getResultCode();
            if(!result.equals(NetworkInterfaceConstants.CHUNK_OK) && !result.equals(NetworkInterfaceConstants.OK))
                break;
            offset += chunkSize;
        }
        inputStream.close();
        return result;
    }


    /**
     * Prints command line usage.
     *
     * @param msg A message to include with usage info.
     */
    private static void usage( String msg )
    {
        System.err.println( msg );
        System.err.println( "Usage: java ServerLoader  <server ip> <magic word>  <number of threads> <number of bulletins>" );
    }


    private MartusSecurity martusCrypto;
    private ClientSideNetworkGateway gateway;
    private String serverIP;
    private String magicWord;
    private ClientBulletinStore store;
    private File tempDir;
    private int numThreads;
    private int numBulletins;
    File[] zippedBulletins;
    UniversalId[] bulletinIds;
}

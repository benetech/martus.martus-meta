package org.martus.loadtest;

import java.io.File;

import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPIWithHelpers;
import org.martus.util.StreamableBase64;

/**
 * @author roms
 *         Date: 1/30/13
 */
public class ServerLoader {

    public ServerLoader(String serverPublicKey, String serverIP, String magicWord, String storeDir)
    {
        this.serverPublicKey = serverPublicKey;
        this.serverIP = serverIP;
        this.magicWord = magicWord;
        this.storeDir = storeDir;
    }

    public final static void main( String[] args )
    {
        if( args.length < 5 )
        {
            usage("Not enough arguments.");
        }
        else {
            String storeDir = args[0];
            String serverIp = args[1];
            String serverPublicKey = args[2];
            String magicWord = args[3];
            int numThreads = Integer.valueOf(args[4]);

            final ServerLoader loader = new ServerLoader(serverPublicKey, serverIp, magicWord, storeDir);
            loader.startLoading();

        }
    }

    public void startLoading()
    {

        try {
            martusCrypto = new MartusSecurity();
            martusCrypto.createKeyPair();

            NonSSLNetworkAPIWithHelpers server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);
            String result = server.getServerPublicKey(martusCrypto);
            if (confirmServerPublicKey(serverPublicKey, result))
            {
                System.out.println("Server info okay");
            } else
            {
                System.out.println("Server info not correct");
                return;
            }

            gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);
            NetworkResponse response = gateway.getUploadRights(martusCrypto, magicWord);
            if (!response.getResultCode().equals(NetworkInterfaceConstants.OK))
            {
                System.out.println("couldn't verify magic word");
                return;
            }

            //can now create bulletins;
        } catch (Exception e) {
            System.err.println("unable to verify server info");
            e.printStackTrace();
        }

        store = new ClientBulletinStore(martusCrypto);
        try
        {
            store.doAfterSigninInitialization(new File(storeDir));
        } catch (Exception e) {
            System.err.println("unable to initialize store");
            e.printStackTrace();
        }

    }

    private boolean confirmServerPublicKey(String serverCode, String serverPublicKey) throws StreamableBase64.InvalidBase64Exception
    {
        final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverCode);
        final String computedCode;
        computedCode = MartusCrypto.computePublicCode(serverPublicKey);
        return normalizedPublicCode.equals(computedCode);
    }


    /**
     * Prints command line usage.
     *
     * @param msg A message to include with usage info.
     */
    private static void usage( String msg )
    {
        System.err.println( msg );
        System.err.println( "Usage: java ServerLoader  /tmp 54.245.101.104 8714.7632.8884.7614.8217 spam 10" );
    }


    private MartusSecurity martusCrypto;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private String serverPublicKey;
    private String magicWord;
    private ClientBulletinStore store;
    private String storeDir;
}

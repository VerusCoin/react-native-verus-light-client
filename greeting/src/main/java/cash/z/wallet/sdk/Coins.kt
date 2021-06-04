package cash.z.wallet.sdk

import android.content.Context
import android.content.SharedPreferences
import cash.z.wallet.sdk.exception.BirthdayException
import cash.z.wallet.sdk.exception.InitializerException
//import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.Identities
import cash.z.wallet.sdk.Initializer.WalletBirthday
import cash.z.wallet.sdk.Synchronizer.AddressType.Shielded
import cash.z.wallet.sdk.Synchronizer.AddressType.Transparent
import cash.z.wallet.sdk.Synchronizer.Status.*
import cash.z.wallet.sdk.block.CompactBlockDbStore
import cash.z.wallet.sdk.block.CompactBlockDownloader
import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.block.CompactBlockProcessor.*
import cash.z.wallet.sdk.block.CompactBlockProcessor.State.*
import cash.z.wallet.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.wallet.sdk.block.CompactBlockStore
import cash.z.wallet.sdk.db.entity.*
import cash.z.wallet.sdk.exception.SynchronizerException
import cash.z.wallet.sdk.ext.ZcashSdk
import cash.z.wallet.sdk.ext.twig
import cash.z.wallet.sdk.ext.twigTask
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.service.LightWalletGrpcService
import cash.z.wallet.sdk.service.LightWalletService
import cash.z.wallet.sdk.transaction.*
import cash.z.wallet.sdk.DemoConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import io.github.novacrypto.bip39.SeedCalculator

import java.io.File


class Coins (
  ticker: String,
  protocol: String,
  accountHash: String,
  iIndexNumber: Int,
  context: Context,
  iviewingkey: String,
  iPort: Int,
  iHost: String,
  iBirthdayString: String,
  iBirthdayInt: Int,
  iSapling: String,
  iNumberOfAccounts: Int
  ){
    //this coin-account syncronizer
     public var synchronizer: Synchronizer? = null;
     //the initialzation of this account, with all relevant data
     public var initializer: Initializer? = null;
     //the client, specific port and host
     public var client: LightWalletService? = null;
     //the index number of this coinId, accounthash, Protocl
     private var indexNumber = -1;
     //the path to this coin-account database
     private var path: String = "";
     //the host of the server to which to talk
     private var host: String = "";
     //the port on which to communicate
     private var port: Int = -1; //67
     //the seed of this account-coin
     //private var seed: String = "";
     //seed in uft8
     //private var seedInUft8: String = "";
     //seed in byte array
     private var viewingkey: String = "";
     //birthday as string
     private var birthdayString: String = "";
     //birthday as int
     private var birthdayInt: Int = -1;
     //birthday wallet bojcet
     private lateinit var birthdayWallet: WalletBirthday;
     //number of accounts
     private var numberOfAccounts: Int = 0;
     //array of all addresses
     private lateinit var addresses: Array<String>;

     private lateinit var identities: ArrayList<Identities>;

     //private
    var syncroStatus: String = "";

     //private
    var syncroProgress: Int = -3;

     var sapling = "";

     var arraylistPending = ArrayList<String>();
    var arraylistReceived = ArrayList<String>();
    var arraylistCleared = ArrayList<String>();
    var arraylistSend = ArrayList<String>();

    companion object {
      public fun toSeed(mnemonic: CharArray): ByteArray {
          return SeedCalculator().calculateSeed(String(mnemonic), "")
      }
    }

    init{ //all the vars are passed into vars
      var name: String = "$ticker _$accountHash _$protocol";
      path = Initializer.dataDbPath(context, name);
      host = iHost;
      port = iPort;
      //seed = iSeed;
      birthdayInt = iBirthdayInt;
      birthdayString = iBirthdayString;
      //seedInUft8 = iSeedInUft8;
      indexNumber = iIndexNumber;
      numberOfAccounts = iNumberOfAccounts;
      viewingkey = iviewingkey;//toSeed(seed.toCharArray());
      birthdayString = birthdayInt.toString();
      sapling = iSapling;
      val file = File("zcash/saplingtree/$birthdayString.json");
      if(file.exists() == true){
        birthdayWallet = Initializer.DefaultBirthdayStore.loadBirthdayFromAssets(context, birthdayInt);
      }else{
        birthdayWallet = Initializer.DefaultBirthdayStore.loadBirthdayFromAssets(context);
      }
    }
    /*Funcitons that allow access to the infroamtion in this object*/
    //gets index number
    public fun getIndexNumberOfCoin(): Int{
      return indexNumber;
    }
    //gets the client and initializes one
    public fun putInitClient(mContext: Context): String{
    try{
      client = LightWalletGrpcService(mContext, host, port);
    }catch(e: Exception){
      return e.toString();
    }
      return "true";
    }

    //gets the addresses
    public fun getAddress(): Array<String>{
      addresses = Array<String>(numberOfAccounts,
        {
          x: Int ->
          initializer?.deriveAddress(viewingkey)!!
        }
        );
      return addresses;
    }

    //gets the privatekey
  /*  functions for internal private key handeling.
      a disciscion was made that they are handeld in the app and not in the lib.
      so have been removed.

    public fun getPrivKey(): String{
      return initializer?.deriveSpendingKeys(seedInByteArray!!)!!.first().toString();
    }
    //gets the spendingKeys
    public fun getSpendingKeys(): String{
      return initializer?.deriveSpendingKeys(seedInByteArray!!)!!.toString();
    }
    //gets the viewingKeys
    public fun getViewingKeys(): String{
      return initializer?.deriveViewingKey(getSpendingKeys())!!;
    }
    */
    //makes a new initialzer
    public fun putInitNew(){
        initializer?.new(viewingkey, birthdayWallet, numberOfAccounts)!!;
        getAddress();
    }

    //opens a new initialzer
    public fun putInitOpen(){
        initializer?.open( birthdayWallet)
        getAddress();
    }
    //gets the number of accounts
    public fun getNumberOfAccounts(): Int{
      return numberOfAccounts;
    }


    //sets the identity
    public fun setIdentity(iIdentity: Identities): Int{
      var indexNumber: Int = 0;
      if(::identities.isInitialized){
        indexNumber = identities.size;
      }
      identities.add(iIdentity);
      return identities.size -1;
      }

    //returns the status
     public fun getStatus(): String{
       return syncroStatus;
     }

     //returns the progress
     public fun getProgress(): Int{
       return syncroProgress;
     }

//monitors the synchronizer changes
    public fun monitorChanges() = runBlocking {
      try{
      //first load the status
      GlobalScope.launch { //has to happen here, becuase java does not have coroutines
          synchronizer?.status!!.collect({ x -> syncroStatus = "$x" });
        }
        //first load the download percentage
        GlobalScope.launch { //has to happen here, becuase java does not have coroutines
            synchronizer?.progress!!.collect({x -> syncroProgress = x;
              //when that is 100 load the scan percentage
              if(syncroProgress == 100){
                  synchronizer?.processorInfo!!.collect({x -> syncroProgress = x.scanProgress });
                } });
          }

        }catch(e: Exception){

      }
    }



    //gets the index corresponding to the address
    public fun getAccountIndex(account: String): Int{
      for ( x in 0 until numberOfAccounts - 1){
        if(addresses[x].equals(account)){
          return x
        }
      }
      return -1;
    }

//test the bytearray to string
    fun test(array: ByteArray): String{
      var string = " "
      var message = " "
      for(byte in array.iterator()){
          var test = byte.toString()
          if(test != "0") {
              val st = String.format("%02X", byte)
              string = string + st
              val ts = byte.toChar()
              message = message + ts
          }
      }

      twig(message)
      twig(string)
        return message
  }

  //load the transactions
    public fun monitorWalletChanges() = runBlocking{
      GlobalScope.launch {
        synchronizer?.receivedTransactions!!.collect(
          {
            x ->
            var meme = x.toList();
            for(p in 0 until meme.size){
              var hex =  meme[p]!!.memo //Charsets.US_ASCII
                var answere = ""
              if(hex != null) {
                answere = test(hex!!)
              }
              val info: String = "address, " + meme[p]!!.toAddress.toString() + ", amount," + meme[p]!!.value.toString() +
              ", category, recieved, status, confirmed, time," + meme[p]!!.blockTimeInSeconds.toString() +" , txid, " +  meme[p]!!.id.toString() + ", height," + meme[p]!!.minedHeight.toString() + ", memo, " + answere + ","
              //here we can add all values together in
              //one big string
              arraylistReceived.add(info)
            }
          }
          );
      }

      GlobalScope.launch {
        synchronizer?.sentTransactions!!.collect({
          x ->
          var meme = x.toList();
          for(p in 0 until meme.size){
            var hex =  meme[p]!!.memo //Charsets.US_ASCII
              var answere = ""
            if(hex != null) {
              answere = test(hex!!)
            }
            val info: String = "address, " + meme[p]!!.toAddress.toString() + ", amount," + meme[p]!!.value.toString() +
            ", category, recieved, status, confirmed, time," + meme[p]!!.blockTimeInSeconds.toString() +" , txid, " +  meme[p]!!.id.toString() + ", height," + meme[p]!!.minedHeight.toString() + ", memo, " + answere + ","
            //here we can add all values together in
            //one big string
            arraylistSend.add(info)
          }
        }
        );
      }

      GlobalScope.launch {
        synchronizer?.pendingTransactions!!.collect(
          {
            x ->
            var meme = x.toList();
            for(p in 0 until meme.size){
              var hex =  meme[p]!!.memo //Charsets.US_ASCII
                var answere = ""
              if(hex != null) {
                answere = test(hex!!)
              }
              val info: String = "address, " + meme[p]!!.toAddress.toString() + ", amount," + meme[p]!!.value.toString() +
              ", category, recieved, status, confirmed, time, , txid, " +  meme[p]!!.id.toString() + ", height," + meme[p]!!.minedHeight.toString() + ", memo, " + answere + ","
              arraylistPending.add(info)
            }
          }
          );
      }



      GlobalScope.launch {
        synchronizer?.clearedTransactions!!.collect({
        x ->
        var meme = x.toList()
        for(p in 0 until meme.size){
          var hex =  meme[p]!!.memo //Charsets.US_ASCII
            var answere = ""
          if(hex != null) {
            answere = test(hex!!)
          }
          val info: String = "address, " + meme[p]!!.toAddress.toString() + ", amount," + meme[p]!!.value.toString() +
          ", category, recieved, status, confirmed, time," + meme[p]!!.blockTimeInSeconds.toString() +" , txid, " +  meme[p]!!.id.toString() + ", height," + meme[p]!!.minedHeight.toString() + ", memo, " + answere + ","
          //here we can add all values together in
          //one big string
          twig("dit runt ook ook ${info}")
          arraylistCleared.add(info)
        }
      }
      );
      }
    }

}

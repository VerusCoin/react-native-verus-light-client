package cash.z.wallet.sdk

import android.content.Context
import android.content.SharedPreferences
import cash.z.wallet.sdk.exception.BirthdayException
import cash.z.wallet.sdk.exception.InitializerException
//import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.Initializer
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
import cash.z.wallet.sdk.rpc.Service

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main

class Identities(){

  private var name: String = "";
  private var contentMap: Map<String, String>? = null;
  private var primaryAddresses: List<String>? = null;
  private var minimumSignatures = -1;
  private var privateAddress = "";
  private var revocationAuthority = "";
  private var recoveryAuthority = "";
  private var flags = -1;
  private var version = -1;
  private var parent = "";
  private var status = "";
  private var canSignFor: Boolean? = null;
  private var canSpendFor: Boolean? = null;
  private var blockHeigt: Long = -1;
  private var txId = "";
  private var vOut = -1;
  private var salt = "";
  private var nameId = "";
  private var referralIdentity = "";

// returns name of the identity
  public fun getName(): String{
    return name;
  }

// returns an dictionary of atributes
  public fun getContentMap(): Map<String, String>?{
    return contentMap;
  }

// returns the addresses of the identity
  public fun getAddresses(): List<String>?{
    return primaryAddresses;
  }
// returns how many signatures you need
  public fun getMinimumSignatures(): Int{
    return minimumSignatures;
  }

//returnes the private address
  public fun getPrivateAddress(): String{
    return privateAddress;
  }

//returnes the authority that can recover
  public fun getRecoveryAuthority(): String{
    return recoveryAuthority;
  }

//returns the flags
  public fun getFlags(): Int{
    return flags;
  }

//returns the version
  public fun getVersion(): Int{
    return version;
  }

//returns the parent
  public fun getParent(): String{
    return parent;
  }

//returns the identities it can sign for
  public fun getCanSignFor(): Boolean?{
    return canSignFor;
  }

//returns the address it can spend for
  public fun getCanSpendFor(): Boolean?{
    return canSpendFor;
  }

//returns the blockcheight it was made
  public fun getBlockHeight(): Long{
    return blockHeigt;
  }

//returns the vouts
  public fun getVout(): Int{
    return vOut;
  }

//returns the txid
  public fun getTxid(): String{
    return txId;
  }

//returns the salt
  public fun getSalt(): String{
    return salt;
  }


// returns the name of the id
  public fun getNameId(): String{
    return nameId;
  }

// returns the referal of the id
  public fun getReferalId(): String{
    return referralIdentity;
  }

//returns the revocation authority
  public fun getRevocationAuthority(): String{
    return revocationAuthority;
  }

//returns the status
  public fun getStatus(): String{
    return status;
  }

//updates the identity
  public fun updateIdentityInfo( identityInfo: Service.IdentityInfo) {
      status = identityInfo.getStatus();
      canSignFor = identityInfo.getCansignfor();
      canSpendFor = identityInfo.getCanspendfor();
      blockHeigt = identityInfo.getBlockheight();
      txId = identityInfo.getTxid();
      vOut = identityInfo.getVout();
  }


//updates name resercation
  public fun updateNameReservation(
    nameReservation: Service.NameReservation
    ){
      name = nameReservation.getName();
      salt = nameReservation.getSalt();
      nameId = nameReservation.getNameid();
      referralIdentity = nameReservation.getReferral();
      parent = nameReservation.getParent();
    }

//makes an idenity kotlin object instead of service
    public fun setIdentity(identity: Service.Identity){
      name = identity.getName();
      contentMap = identity.getContentmap();
      revocationAuthority = identity.getRevocationauthority();
      primaryAddresses = identity.getPrimaryaddressesList();
      minimumSignatures = identity.getMinimumsignatures();
      privateAddress = identity.getPrivateaddress();
      recoveryAuthority = identity.getRecoveryauthority();
      flags = identity.getFlags();
      version = identity.getVersion();
      parent = identity.getParent();
    }

}

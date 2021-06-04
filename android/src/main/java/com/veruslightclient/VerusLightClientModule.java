package com.veruslightclient;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.IllegalViewOperationException;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;


import android.content.Context;
import android.app.Activity;
import android.app.Application;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

//sdk classes
import cash.z.wallet.sdk.Initializer;
import cash.z.wallet.sdk.Initializer.WalletBirthday;
import cash.z.wallet.sdk.DemoConfig;
import cash.z.wallet.sdk.Synchronizer;
import cash.z.wallet.sdk.Synchronizer.Status.*;
import cash.z.wallet.sdk.service.LightWalletService;
//import cash.z.wallet.sdk.db.entity.isFailure;
import cash.z.wallet.sdk.transaction.*;
import cash.z.wallet.sdk.ext.*;
import cash.z.wallet.sdk.jni.RustBackend;
import cash.z.wallet.sdk.service.LightWalletService;
import cash.z.wallet.sdk.service.LightWalletGrpcService;
import cash.z.wallet.sdk.KtJavaComLayer;
import cash.z.wallet.sdk.Identities;


class VerusLightClientModule extends ReactContextBaseJavaModule {
	/* mandatory react stuff*/
	public VerusLightClientModule(@Nonnull ReactApplicationContext reactContext) {
        super(reactContext);
	}

 private static final String E_LAYOUT_ERROR = "E_LAYOUT_ERROR";

	@Nonnull
    @Override
    public String getName() {
        return "VerusLightClient";
    }

/*Callback interface*/


	/* private veriables for Kotlin */
	private static Context context; //holds application context
	private static byte[] SeedinByteArrayuft8; //hold the SeedinByteArrayuft8
	private static Map<String, Integer> coinToIndex = new HashMap<String, Integer>();


	/*React method*/
	/*the request method is the main way to interact with the kotlin*/

	@ReactMethod
	public void request (int id, String method, ReadableArray params, Promise promise){
		try{
		JSONObject response = new JSONObject();
		String result = "";
		String error = "";

		//index number to link correct object to the function
		//params: coinId, accountHash, coinProtocol
		int index = getIndex(params.getString(0), params.getString(1), params.getString(2));

		//todo object creation conform standard
		switch(method){
			case "getblockcount":
				result = this.getBlockCount(index);
			try{
				response.put("result", result);

			}catch(JSONException e ){
				result = e.toString();
			}
				break;

			case "getblockrange":
			/*
			param 4: startblockinded
			param 5: stop blockindex
			*/
				result = this.getBlockRange(params.getString(3), params.getString(4), index);
				break;

			case "getblockinfo":
			/*
			param 4: blockindex
			*/
				result = this.getBlockInfo( params.getString(3), index);
				break;

			case "listaddresses":
			try{
				String[] test = this.getAddress(index);
				JSONArray array = new JSONArray();

				for(int x = 0; x < test.length; x++){
					array.put(test[x]);
				}

				response.put("result", array);
			}catch(JSONException e ){
				result = e.toString();
			}
				break;


			case "getidentities":

				JSONObject id2 = this.getId(index, params.getString(3));

				if( id2 == null){
						result = "error: name not found";
				}else{
				try{
					response.put("result", id2);

				}catch(JSONException e ){
					result = e.toString();
				}
			}
				break;

			case "getidentitieswithinfo":

				JSONObject objJSON = this.getIdwithInfo(index, params.getString(3));

				if( objJSON == null){
						result = "error: name not found";
				}else{
					try{
						response.put("result", objJSON);

					}catch(JSONException e ){
						result = e.toString();
					}
				}
				break;
			case "verifymessage":

				try{

					response.put("result", this.verifyMessage(index, params.getString(3), params.getString(4), params.getString(5), params.getString(6)));

				}catch(JSONException e ){
					result = e.toString();
				}
				break;

			case "listprivatetransactions":
			/*
			param 4: "pending" OR "cleared" OR "received" OR "sent" OR "all"
			*/
			//pending not done yet.
			String [] composite;
			if(params.size() < 4){

				String[] one = this.getListOfTransaction("recieved", index);
				String[] two = this.getListOfTransaction("pending", index);
				String[] three = this.getListOfTransaction("send", index);

				composite = new String[one.length + two.length + three.length];

				for(int x = 0; x < one.length; x++){
					composite[x] = one[x];
				}

				for(int x = 0; x < two.length; x++){
					composite[one.length + x] = two[x];
				}

				for(int x = 0; x < three.length; x++){
					composite[one.length + two.length + x] = three[x];
				}

			}else{
			if(params.getString(3) != "all" || params.getString(3) != ""){

				composite = this.getListOfTransaction(params.getString(3), index);

			}else{
					//String[] four = this.getListOfTransaction("cleared", index);
					//memes = new String[localMeme.length];

					String[] one = this.getListOfTransaction("recieved", index);
					String[] two = this.getListOfTransaction("pending", index);
					String[] three = this.getListOfTransaction("send", index);

					composite = new String[one.length + two.length + three.length];

					for(int x = 0; x < one.length; x++){
						composite[x] = one[x];
					}

					for(int x = 0; x < two.length; x++){
						composite[one.length + x] = two[x];
					}

					for(int x = 0; x < three.length; x++){
						composite[one.length + two.length + x] = three[x];
					}

			}
		}
			try{

				JSONArray array = new JSONArray();

				for(int x = 0; x < composite.length; x++){
					JSONObject returned = new JSONObject();
					cutString(composite[x], returned, 0);
				}

				for(int x = 0; x < composite.length; x++){
					array.put(composite[x]);
				}

				response.put("result", array);
			}catch(JSONException e ){
				result = e.toString();
			}
				break;
			case "send":
			/*
			param 4: toAddress,
			param 5: fromAddress,
			param 6: amount,
			param 7: memo
			*/
				result = this.putSend(params.getString(3), params.getString(4), Long.parseLong(params.getString(5)), params.getString(6), params.getString(7), index);

				try{
					response.put("result", result);
				}catch(JSONException e ){
					result = e.toString();
				}

				break;

			case "getinfo":

			String status;
			Integer progress;
			Integer blockcount;
			String blockCountStr;

				status = this.getSyncStatus(index);
				progress = this.getSyncprogress(index);
				blockCountStr = this.getBlockCount(index);

				if(blockCountStr != "error: not initialized coin usage"){
					blockcount = Integer.parseInt(blockCountStr);

				try{

					JSONObject info = new JSONObject();
					info.put("status", status);
					info.put("percent", progress);
					info.put("blocks", blockcount);

					response.put("result", info);
				}catch(JSONException e ){
					result = e.toString();
				}

			}else{

				result = blockCountStr;

			}
				break;

			case "z_getbalance":

			if(params.size() > 3){
				result = this.getBalance(params.getString(3), "", index);
			}else if (params.size() > 4) {
				result = this.getBalance(params.getString(3), params.getString(4), index);
			}else{
				result = this.getBalance("true", "", index);
			}

			int character = result.indexOf(',');

			if(character != -1){

			String totalBalanceStr = result.substring(0, character - 1);
			Double totalBalanceDbl =  Double.parseDouble(totalBalanceStr);

			String confirmedBalanceStr = result.substring(character + 1, result.length());
			Double confirmedBalanceDbl =  Double.parseDouble(confirmedBalanceStr);

			JSONObject balance = new JSONObject();

			try{

				balance.put("total", totalBalanceDbl);
				balance.put("confirmed", confirmedBalanceDbl);
				response.put("result", balance);

			}catch(JSONException e ){

			}
		}
					break;
			case "getprivatebalance":
			/*
			param 4: includePending: boolstring* (optional),
			param 5: address: String (optional)
			*/
				if(params.size() > 3){
					result = this.getBalance(params.getString(3), "", index);
				}else if (params.size() > 4) {
					result = this.getBalance(params.getString(3), params.getString(4), index);
				}else{
					result = this.getBalance("true", "", index);
				}

				int indexOfComma = result.indexOf(',');
				JSONObject balanceStonks = new JSONObject();
				String totalBalanceStonksStr;
				Double totalBalanceStonksDbl;

				String confirmedBalanceStonksStr;
				Double confirmedBalancDbl;

				String errorResponse;
				try{
				if(indexOfComma != -1){

					totalBalanceStonksStr = result.substring(0, indexOfComma - 1);
					totalBalanceStonksDbl = Double.parseDouble(totalBalanceStonksStr);
					confirmedBalanceStonksStr = result.substring(indexOfComma + 1, result.length());
					confirmedBalancDbl = Double.parseDouble(confirmedBalanceStonksStr);

					balanceStonks.put("total", totalBalanceStonksDbl);
					balanceStonks.put("confirmed", confirmedBalancDbl);
					response.put("result", balanceStonks);

				}else{
					errorResponse = "error: invalid balanced recieved";
					response.put("result", errorResponse);
				}
				}catch(JSONException e ){
					result = e.toString();
				}
			break;
			default:
			error = "-32601	Method not found	The method does not exist / is not available";
		}


try {

		if(result.length() > 5){
			String test = result.substring(0, 6);

			if(test.equals("error:")){

				JSONObject errorOBJ = new JSONObject();
				errorOBJ.put("code" , "-32603");
				error = result.substring(7, result.length());

				errorOBJ.put("message", "Invalid Paramester");
				errorOBJ.put("data", error);
				response.put("result", JSONObject.NULL );
				response.put("error", errorOBJ);

			}else{
				response.put("error", error);
			}
		}

		if(!response.has("result")){
			response.put("result", result);
		}
		response.put("id", id);
		response.put("jsonrpc", "2.0");
		/*id, result, error, version*/
		} catch (JSONException e) {

		}
		promise.resolve(response.toString());
	}catch (IllegalViewOperationException e) {
		promise.reject(E_LAYOUT_ERROR, e);
	}
	}

	/*Helper Methods
	these fucntions should only be called form the request function. The request
	funtion is the primary way to interact with the module from js.
	*/

	//gets the blockcount
	private String getBlockCount(int index) {
		return cash.z.wallet.sdk.KtJavaComLayer.Companion.getBlockHeightDirty(VerusLightClientModule.context, index);
	}

	/*context: Context, index: Int, txId: String, feeoffer: Double, name: String, contentmap: Map<String, String>,
			primaryaddresses: List<String>, minimumSignatures: Int, privateAddress: String, revocationAuthority: String, recoveryAuthority: String,
			flags: Int, version: Int, parent: String, salt: String, nameId: String, referralIdentity: String*/


	private JSONObject getIdwithInfo(int index, String identity){
		JSONObject identityInfo = new JSONObject();
		JSONObject identityObj = new JSONObject();
		try{

		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

		Identities id = cash.z.wallet.sdk.KtJavaComLayer.Companion.getIdentityDirty(mContext, index, identity);

		if(id == null){
				return null;
		}

		identityObj.put("name", id.getName());

		JSONObject contentMap = new JSONObject();

		for (String key : id.getContentMap().keySet()) {
				contentMap.put(key, id.getContentMap().get(key));
		}

		JSONObject primaryAddresses = new JSONObject();

		primaryAddresses.put("primaryaddresses", id.getAddresses());
		identityObj.put("primaryaddresses", primaryAddresses);
		identityObj.put("contentmap", contentMap);
		identityObj.put("minimumSignatures", id.getMinimumSignatures());
		identityObj.put("privateAddress", id.getPrivateAddress());
		identityObj.put("revocationAuthority", id.getRevocationAuthority());
		identityObj.put("recoveryAuthority", id.getRecoveryAuthority());
		identityObj.put("flags", id.getFlags());
		identityObj.put("version", id.getVersion());
		identityObj.put("parent", id.getParent());

		//now the info part
		Identities info = cash.z.wallet.sdk.KtJavaComLayer.Companion.getIdentityInfoDirty(mContext, index, identity);

		identityInfo.put("status", info.getStatus());
		identityInfo.put("canSignFor", info.getCanSignFor().toString());
		identityInfo.put("canSpendFor", info.getCanSpendFor().toString());
		identityInfo.put("blockHeight", info.getBlockHeight());
		identityInfo.put("txId", info.getTxid());
		identityInfo.put("vOut", info.getVout());

		identityInfo.put("identity", identityObj);
	}catch(JSONException e){
		//F
	}
		return identityInfo;
	}

	private JSONObject getId(int index, String identity){
		JSONObject identityObj = new JSONObject();
		try{

		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

		Identities id = cash.z.wallet.sdk.KtJavaComLayer.Companion.getIdentityDirty(mContext, index, identity);

		if(id == null){
				return null;
		}

		identityObj.put("name", id.getName());

		JSONObject contentMap = new JSONObject();

		for (String key : id.getContentMap().keySet()) {
        contentMap.put(key, id.getContentMap().get(key));
    }

		JSONObject primaryAddresses = new JSONObject();

		primaryAddresses.put("primaryaddresses", id.getAddresses());
		identityObj.put("primaryaddresses", primaryAddresses);
		identityObj.put("contentmap", contentMap);
		identityObj.put("minimumSignatures", id.getMinimumSignatures());
		identityObj.put("privateAddress", id.getPrivateAddress());
		identityObj.put("revocationAuthority", id.getRevocationAuthority());
		identityObj.put("recoveryAuthority", id.getRecoveryAuthority());
		identityObj.put("flags", id.getFlags());
		identityObj.put("version", id.getVersion());
		identityObj.put("parent", id.getParent());

	}catch(JSONException e){
		//F
	}
		return identityObj;
	}

	//gets a block range, this is basicaly blockinfo on 2 blocks
	private String getBlockRange(String startRange, String stopRange, int index){

		int startRangeInt = Integer.parseInt(startRange);
		int stopRangeInt = Integer.parseInt(stopRange);

		return cash.z.wallet.sdk.KtJavaComLayer.Companion.getBlockRangeDirty(VerusLightClientModule.context, startRangeInt, stopRangeInt, index);
	}

	//gets blockinfo, header, hash, input output etc
	private String getBlockInfo(String blockNumber, int index){

		int blockNumberInt = Integer.parseInt(blockNumber);

		return cash.z.wallet.sdk.KtJavaComLayer.Companion.getBlockDirty(VerusLightClientModule.context, blockNumberInt, index);
	}

//verifies a message and returns a bolean in string form
	private String verifyMessage(int index, String signer, String signature, String message, String checklast){

		Boolean check = Boolean.parseBoolean(checklast);
		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

		return cash.z.wallet.sdk.KtJavaComLayer.Companion.verifyMessageDirty(mContext, index, signer, signature, message, check).toString();
	}

	//calculates your address for you from your seed
	private String[] getAddress(int index){
			return cash.z.wallet.sdk.KtJavaComLayer.Companion.getAddressDirty(index);
	}



	//lists all transactions assosiated with your private key
	private String[] getListOfTransaction(String info, int index){

		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

			return cash.z.wallet.sdk.KtJavaComLayer.Companion.getListOfTransactionDirty(VerusLightClientModule.context, info, index);
	}

	//gets the balance of the syncronized wallet
	private String getBalance(String includePendingStr, String address, int index){

		boolean includePending;

		if(includePendingStr.equals("")){
			includePending = true;
		}else{
			includePending = Boolean.parseBoolean(includePendingStr);
		}

		return  cash.z.wallet.sdk.KtJavaComLayer.Companion.getWalletBalanceDirty(includePending, address, index);
	}

	//sends a message
	private String putSend(String toAddress, String fromAddress, Long amount, String spendingKey, String memo, int index){
		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

			return cash.z.wallet.sdk.KtJavaComLayer.Companion.putSendDirty(VerusLightClientModule.context, toAddress, fromAddress, amount, spendingKey, memo, index);
	}

	//gets the status of the syncronizer, this changes all the time
	private String getSyncStatus(int index){
			return cash.z.wallet.sdk.KtJavaComLayer.Companion.getSyncStatusDirty(index);
	}
//gets the progress the syncronizer has made in downloading the relevant blocks
	private Integer getSyncprogress(int index){
		return cash.z.wallet.sdk.KtJavaComLayer.Companion.getSyncProgressDirty(index);
	}


/*Initialzing methods*/

	@ReactMethod //initialzes/new the back end, data base etc
	//only use this if the database has never been opened

	/*
		use this method to load, all the data into the coin object. If you call this method Nothing
		is started yet. It only loads the data into the relevant objects. If you also want to controll the zcash protocol
	*/
	public void createWalletsap ( String coinId, String coinProtocol, String accountHash, String host, int port,
		int numberOfAccounts, String viewkey, int birthday, String sapling, Promise promise ) {
		try{
		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

		String path = coinId + "_" + accountHash + "_" + coinProtocol;
		//String host =  "lightwalletd.z.cash";
		//int port = 9067;
		//String seed = "urban kind wise collect social marble riot primary craft lucky head cause syrup odor artist decorate rhythm phone style benefit portion bus truck top";
		//String seedInUft8 = "dXJiYW4ga2luZCB3aXNlIGNvbGxlY3Qgc29jaWFsIG1hcmJsZSByaW90IHByaW1hcnkgY3JhZnQgbHVja3kgaGVhZCBjYXVzZSBzeXJ1cCBvZG9yIGFydGlzdCBkZWNvcmF0ZSByaHl0aG0gcGhvbmUgc3R5bGUgYmVuZWZpdCBwb3J0aW9uIGJ1cyB0cnVjayB0b3A=";
		String seed = "";
		int birthdayInt = birthday;
		String birthdayString = turnIntIntoBirthdayString(birthday);
		//int numberOfAccounts = 1;

		int indexNumber = cash.z.wallet.sdk.KtJavaComLayer.Companion.addCoin(coinId, accountHash, coinProtocol, VerusLightClientModule.context, viewkey, host, port, seed, birthdayString, birthdayInt, sapling, numberOfAccounts);
		coinToIndex.put(path, indexNumber);
		promise.resolve("true");
	}catch (IllegalViewOperationException e) {
			promise.reject(E_LAYOUT_ERROR, e);
		}
	}

	@ReactMethod //initialzes/new the back end, data base etc
	/*
		use this method to load, all the data into the coin object. If you call this method Nothing
		is started yet. It only loads the data into the relevant objects.
	*/
	public void createWallet ( String coinId, String coinProtocol, String accountHash, String host, int port,
	 	int numberOfAccounts, String viewingkey, int birthday, Promise promise ) {
		try{
		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

		String path = coinId + "_" + accountHash + "_" + coinProtocol;
		//String host =  "lightwalletd.z.cash";
		//int port = 9067;
		//String seed = "urban kind wise collect social marble riot primary craft lucky head cause syrup odor artist decorate rhythm phone style benefit portion bus truck top";
		//String seedInUft8 = "dXJiYW4ga2luZCB3aXNlIGNvbGxlY3Qgc29jaWFsIG1hcmJsZSByaW90IHByaW1hcnkgY3JhZnQgbHVja3kgaGVhZCBjYXVzZSBzeXJ1cCBvZG9yIGFydGlzdCBkZWNvcmF0ZSByaHl0aG0gcGhvbmUgc3R5bGUgYmVuZWZpdCBwb3J0aW9uIGJ1cyB0cnVjayB0b3A=";
		String seed = "";
		int birthdayInt = birthday;
		String birthdayString = turnIntIntoBirthdayString(birthday);
		//int numberOfAccounts = 1;

		int indexNumber = cash.z.wallet.sdk.KtJavaComLayer.Companion.addCoin(coinId, accountHash, coinProtocol, VerusLightClientModule.context, viewingkey, host, port, seed, birthdayString, birthdayInt, "sapling", numberOfAccounts);
		coinToIndex.put(path, indexNumber);
		promise.resolve("true");
	}catch (IllegalViewOperationException e) {
			promise.reject(E_LAYOUT_ERROR, e);
		}
	}


	/*
		this function derives the spendingkeys from the seed.
	*/

	@ReactMethod
	public void deriveSpendingKeys(String seed, Boolean iets, int numberOfAccounts, Promise promise){
		try{
			Activity mActivity = getCurrentActivity();
			Context mContext = mActivity.getApplicationContext();
			String[] response = cash.z.wallet.sdk.KtJavaComLayer.Companion.getderiveSpendingKeys(seed, numberOfAccounts, mContext);
			JSONArray array = new JSONArray();

			for(int x = 0; x < response.length; x++){
				array.put(response[x]);
			}
			promise.resolve(array.toString());
		}catch (IllegalViewOperationException e) {
				promise.reject(E_LAYOUT_ERROR, e);
			}
	}


	/*
		this function derives the viewing keys from a spendingkey.
	*/
	@ReactMethod
	public void deriveViewingKey(String spendingKey, Promise promise){
		try{
			Activity mActivity = getCurrentActivity();
			Context mContext = mActivity.getApplicationContext();
			String response = cash.z.wallet.sdk.KtJavaComLayer.Companion.getderiveViewingKey(spendingKey, mContext);
			promise.resolve(response);
		}catch (IllegalViewOperationException e) {
			promise.reject(E_LAYOUT_ERROR, e);
		}
	}

	/*
	this funciton initializes the initializer. THis actually start stuff up, and does nto only load data.
	*/
	@ReactMethod
	public void openWallet(String coinId, String coinProto, String accountHash, String chaintype, Promise promise){
		try{

		int index = getIndex(coinId, coinProto, accountHash); //index number to link correct object to the function
		String path = coinId + "_" + accountHash + "_" + coinProto;

		String response = cash.z.wallet.sdk.KtJavaComLayer.Companion.Initer(VerusLightClientModule.context, path, index, chaintype);

		if(checkError(response) == true){
			promise.reject(response);
		}

		promise.resolve(response);

	}catch (IllegalViewOperationException e) {
		promise.reject(E_LAYOUT_ERROR, e);
	}
	}

	/*
	this funciton initializes the cliet. is is always done automatically in the sdk, however it is there
	for testing perposes etc.
	*/

		@ReactMethod //initialzes the client, this happens automatically too when a fucntion using the
		//client is called. However it is in here too.
		public void initClient (String coinId, String coinProto, String accountHash, Promise promise){
		try{
			Activity mActivity = getCurrentActivity();
			Context mContext = mActivity.getApplicationContext();
			VerusLightClientModule.context = mContext;

			int index = getIndex(coinId, coinProto, accountHash); //index number to link correct object to the function

			String response = cash.z.wallet.sdk.KtJavaComLayer.Companion.InitClient(VerusLightClientModule.context, index);
			if(checkError(response) == true){
				promise.reject(response);
			}
			promise.resolve(response);
		}catch (IllegalViewOperationException e) {
      promise.reject(E_LAYOUT_ERROR, e);
    }
	}

	/* starts the syncronizer, used for interacting with the blockchain */
		@ReactMethod
		public void startSync( String coinId, String coinProto, String accountHash, Promise promise ) {
		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;

		String output = "";

		int index = getIndex(coinId, coinProto, accountHash); //index number to link correct object to the function

		if(index == -1){
			output = "Error: " + coinId + "_" + accountHash + "_" + coinProto + " not initialized";
		}

		output = cash.z.wallet.sdk.KtJavaComLayer.Companion.syncronizerstart(VerusLightClientModule.context, index).toString();

		if(checkError(output) == true){
			promise.reject(output);
		}

		promise.resolve(output);
	}

	//stops the syncronizer
	@ReactMethod
		public void stopSync(String coinId, String coinProtocol, String accountHash, Promise promise){
		try{
			int index = getIndex(coinId, coinProtocol, accountHash); //index number to link correct object to the function
			String output = cash.z.wallet.sdk.KtJavaComLayer.Companion.syncronizerStop(index);

			if(checkError(output) == true){
				promise.reject(output);
			}

			promise.resolve(output);

		}catch (IllegalViewOperationException e) {
			promise.reject(E_LAYOUT_ERROR, e);
		}
	}

	/*
	this is a usefull function in swift. However in android it does not add a lot of value, because you
	can't stop the blockproducer, and keep the syncronizer running on android.
	So here it is the same as stop synchronzer
	*/
	@ReactMethod
		public void closeWallet(String coinId, String coinProtocol, String accountHash,Promise promise){
			try{
				//vars (coinId: String, coinProtocol: String, accountHash: String)
			int index = getIndex(coinId, coinProtocol, accountHash); //index number to link correct object to the function
			String output = cash.z.wallet.sdk.KtJavaComLayer.Companion.syncronizerStop(index);

			if(checkError(output) == true){
				promise.reject(output);
			}

			promise.resolve(output);
		}catch (IllegalViewOperationException e) {
      promise.reject(E_LAYOUT_ERROR, e);
    }
	}

	/*
	this deleles everything that has to do with this wallet. So, ofcourse the objects,
	the coin object, the initializer, the syncronizer, but also the database.
	*/
	@ReactMethod
	public void deleteWallet(String coinId, String coinProtocol, String accountHash,Promise promise){
		try{
			int index = getIndex(coinId, coinProtocol, accountHash); //index number to link correct object to the function
			String output = cash.z.wallet.sdk.KtJavaComLayer.Companion.interDelete(index);

			if(checkError(output) == true){
				promise.reject(output);
			}

			promise.resolve(output);

		}catch (IllegalViewOperationException e) {
      promise.reject(E_LAYOUT_ERROR, e);
    }
	}

	/*helper methods*/
	//this returns the context
	private void setContext(){
		Activity mActivity = getCurrentActivity();
		Context mContext = mActivity.getApplicationContext();
		VerusLightClientModule.context = mContext;
	}
	//this takes the birthday int and turnes it into the correct type of string.
	private String turnIntIntoBirthdayString(int birthday){
		String stringifiedBirthday = Integer.toString(birthday);
		String stringofBirthdayCopy = "";
		int lengthOfBirthday = stringifiedBirthday.length();
		int lengthmodulo3 = lengthOfBirthday%3;

		if(lengthmodulo3 != 0){
			String subString = stringifiedBirthday.substring( 0, lengthmodulo3);
			stringofBirthdayCopy = subString + "_";
		}

		for (int i = 0; i < lengthOfBirthday/3; i++) {
			String subString = stringifiedBirthday.substring(lengthmodulo3 + (i * 3), lengthmodulo3 + (i * 3) + 3);

			if(i != (lengthOfBirthday/3) - 1){
				stringofBirthdayCopy = stringofBirthdayCopy + subString + "_";
			}else{
				stringofBirthdayCopy = stringofBirthdayCopy + subString;
			}

		}

		return stringofBirthdayCopy;
	}
//this gets the index number of the object corresponding with this account,
//coin, and protocol. In kotlin there is an array with coin objects, that are almost
//data objects holding all the relevent information to use multpile coins, crisscross
	private int getIndex(String coinId, String coinProto, String accountHash){
		String name = coinId + "_" + accountHash + "_" + coinProto;

		if(coinToIndex.containsKey(name)){
			return coinToIndex.get(name);
		}else{
			return -1;
		}

	}

	/*Test methds*/
	//you can use this to check the path of your database
	@ReactMethod
	public void getPath(Promise promise){
		try{
			String alias = "Dank memes";
			String path = cash.z.wallet.sdk.KtJavaComLayer.Companion.getPath(VerusLightClientModule.context, alias);

			promise.resolve(path);

		}catch (IllegalViewOperationException e) {
			promise.reject(E_LAYOUT_ERROR, e);
		}
	}

	/* util methods*/
	//you can use this to make a byte array out of a string
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
								 + Character.digit(s.charAt(i+1), 16));
		}

		return data;
	}

	Boolean checkError(String error){
		if(error.length() > 5){

			String test = error.substring(0, 6);

			if(test.equals("error:")){
				return true;
			}

		}
		return false;
	}

	//function that is used to cut a string into all components
	//seperated by comma's and add them together to form a key value pair.
	void cutString(String cutter, JSONObject store, int count){
		int index = 0;
		if(cutter.indexOf(',', index) != -1){
			int indexOne = cutter.indexOf(',', index);

			count = count + 1;
			index = indexOne + 1;

			int indexTwo = cutter.indexOf(',', index);

			if(indexTwo == -1){
				indexTwo = cutter.length() -1 ;
			}

			count = count + 1;
			index = indexTwo + 1;

			String one = cutter.substring(0, indexOne - 1);
			String two = cutter.substring(indexOne + 1, indexTwo - 1);
			String pass = cutter.substring(indexTwo + 1, cutter.length());

			try{
				if(count == 4 || count == 14){
					int integer = Integer.parseInt(two);

					store.put(one, integer);
				}else{
					store.put(one, two);
				}
			}catch(JSONException e ){
				return;
			}
			cutString(pass, store, count);
		}
		return;
	}

}

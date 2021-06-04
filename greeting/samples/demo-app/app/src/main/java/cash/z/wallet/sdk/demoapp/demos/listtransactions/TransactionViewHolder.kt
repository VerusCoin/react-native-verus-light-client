package cash.z.wallet.sdk.demoapp.demos.listtransactions

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cash.z.wallet.sdk.demoapp.R
import cash.z.wallet.sdk.db.entity.ConfirmedTransaction
import cash.z.wallet.sdk.ext.convertZatoshiToZecString
import cash.z.wallet.sdk.ext.twig
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and

/**
 * Simple view holder for displaying confirmed transactions in the recyclerview.
 */
class TransactionViewHolder<T : ConfirmedTransaction>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val timeText = itemView.findViewById<TextView>(R.id.text_transaction_timestamp)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())


    fun test(array: ByteArray){
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
    }

    fun bindTo(transaction: T?) {

        val hex = transaction?.memo //Charsets.US_ASCII
        if(hex != null) {
            test(hex!!)
        }
        amountText.text = transaction?.value.convertZatoshiToZecString()
        timeText.text =
            if (transaction == null || transaction?.blockTimeInSeconds == 0L) "Pending"
            else formatter.format(transaction.blockTimeInSeconds * 1000L)
    }
}
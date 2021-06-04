package cash.z.wallet.sdk.demoapp.demos.getaddress

import android.view.LayoutInflater
import cash.z.wallet.sdk.Initializer
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetAddressBinding

/**
 * Displays the address associated with the seed defined by the default config. To modify the seed
 * that is used, update the `DemoConfig.seedWords` value.
 */
class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private var seed: ByteArray = App.instance.defaultConfig.seed
    private val initializer: Initializer = Initializer(App.instance, "chris", "VRSC")

    private lateinit var address: String

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding
            = FragmentGetAddressBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        var key = initializer.deriveSpendingKeys(seed)
        var viewingkey = initializer.deriveViewingKey(key[0])
        address = initializer.deriveAddress(viewingkey)
    }

    override fun onResetComplete() {
        binding.textInfo.text = address
    }

    override fun onActionButtonClicked() {
        copyToClipboard(address)
    }

}
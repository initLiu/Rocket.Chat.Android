package chat.rocket.android.fragment.server_config

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.TextView

import java.util.HashMap

import chat.rocket.android.R
import chat.rocket.android.api.MethodCallHelper
import chat.rocket.android.layouthelper.oauth.OAuthProviderInfo
import chat.rocket.android.log.RCLog
import chat.rocket.core.models.LoginServiceConfiguration
import chat.rocket.persistence.realm.repositories.RealmLoginServiceConfigurationRepository
import chat.rocket.persistence.realm.repositories.RealmPublicSettingRepository

/**
 * Login screen.
 */
class LoginFragment : AbstractServerConfigFragment(), LoginContract.View {

    private var presenter: LoginContract.Presenter? = null
    private var container: ConstraintLayout? = null
    private var waitingView: View? = null
    private var txtUsername: TextView? = null
    private var txtPasswd: TextView? = null

    override fun getLayout(): Int {
        return R.layout.fragment_login
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = LoginPresenter(
                RealmLoginServiceConfigurationRepository(hostname),
                RealmPublicSettingRepository(hostname),
                MethodCallHelper(context, hostname)
        )
    }

    override fun onSetupView() {
        container = rootView.findViewById(R.id.container)

        val btnEmail = rootView.findViewById<Button>(R.id.btn_login_with_email)
        val btnUserRegistration = rootView.findViewById<Button>(R.id.btn_user_registration)
        txtUsername = rootView.findViewById(R.id.editor_username)
        txtPasswd = rootView.findViewById(R.id.editor_passwd)
        waitingView = rootView.findViewById(R.id.waiting)

        btnEmail.setOnClickListener { view -> presenter!!.login(txtUsername!!.text.toString(), txtPasswd!!.text.toString()) }

        btnUserRegistration.setOnClickListener { view ->
            UserRegistrationDialogFragment.create(hostname, txtUsername!!.text.toString(), txtPasswd!!.text.toString())
                    .show(fragmentManager!!, "UserRegistrationDialogFragment")
        }
    }

    override fun showLoader() {
        container!!.visibility = View.GONE
        waitingView!!.visibility = View.VISIBLE
    }

    override fun hideLoader() {
        waitingView!!.visibility = View.GONE
        container!!.visibility = View.VISIBLE
    }

    override fun showError(message: String) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun showLoginServices(loginServiceList: List<LoginServiceConfiguration>) {
        val viewMap = HashMap<String, View>()
        val supportedMap = HashMap<String, Boolean>()
        for (info in OAuthProviderInfo.LIST) {
            viewMap.put(info.serviceName, rootView.findViewById(info.buttonId))
            supportedMap.put(info.serviceName, false)
        }

        for (authProvider in loginServiceList) {
            for (info in OAuthProviderInfo.LIST) {
                if (!supportedMap[info.serviceName] && info.serviceName == authProvider.service) {
                    supportedMap.put(info.serviceName, true)
                    viewMap[info.serviceName].setOnClickListener { view ->
                        var fragment: Fragment? = null
                        try {
                            fragment = info.fragmentClass.newInstance()
                        } catch (exception: Exception) {
                            RCLog.w(exception, "failed to build new Fragment")
                        }

                        if (fragment != null) {
                            val args = Bundle()
                            args.putString("hostname", hostname)
                            fragment.arguments = args
                            showFragmentWithBackStack(fragment)
                        }
                    }
                    viewMap[info.serviceName].setVisibility(View.VISIBLE)
                }
            }
        }

        for (info in OAuthProviderInfo.LIST) {
            if (!supportedMap[info.serviceName]) {
                viewMap[info.serviceName].setVisibility(View.GONE)
            }
        }
    }

    override fun showTwoStepAuth() {
        showFragmentWithBackStack(TwoStepAuthFragment.create(
                hostname, txtUsername!!.text.toString(), txtPasswd!!.text.toString()
        ))
    }

    override fun onResume() {
        super.onResume()
        presenter!!.bindView(this)
    }

    override fun onPause() {
        presenter!!.release()
        super.onPause()
    }
}
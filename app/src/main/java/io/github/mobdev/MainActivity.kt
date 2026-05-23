package io.github.mobdev

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import io.github.mobdev.network.TokenStore
import io.github.mobdev.ui.AppViewModel
import io.github.mobdev.ui.ChatListFragment
import io.github.mobdev.ui.ChatListViewModel
import io.github.mobdev.ui.ImageFragment
import io.github.mobdev.ui.LoginFragment
import io.github.mobdev.ui.MessagesFragment
import io.github.mobdev.ui.MessagesViewModel
import io.github.mobdev.ui.SelectChatFragment
import io.github.mobdev.ui.VMFactory

class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    val vmFactory: VMFactory by lazy { VMFactory(TokenStore(this), applicationContext) }

    private val isLandscape: Boolean
        get() = findViewById<android.view.View?>(R.id.container_detail) != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val tokenStore = TokenStore(this)
            if (tokenStore.token != null) {
                showChats()
            } else {
                showLogin()
            }
        } else {
            restoreAfterRotation()
        }

        setupBackPressed()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fm = supportFragmentManager

                if (isLandscape) {
                    val topDetail = fm.findFragmentById(R.id.container_detail)
                    if (topDetail is ImageFragment) {
                        fm.popBackStack()
                        return
                    }
                    val selected = appViewModel.selectedChannel.value
                    if (selected != null) {
                        appViewModel.selectChannel(null)
                        fm.commit {
                            replace(R.id.container_detail, SelectChatFragment())
                        }
                        return
                    }
                    finish()
                } else {
                    if (fm.backStackEntryCount > 0) {
                        fm.popBackStack()
                    } else {
                        finish()
                    }
                }
            }
        })
    }

    private fun restoreAfterRotation() {
        val tokenStore = TokenStore(this)
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
        if (tokenStore.token == null) {
            showLogin()
            return
        }
        if (isLandscape) {
            ensureLandscapeContainers(appViewModel.selectedChannel.value)
        } else {
            val channel = appViewModel.selectedChannel.value
            supportFragmentManager.commit {
                replace(R.id.container, ChatListFragment())
            }
            if (channel != null) {
                supportFragmentManager.commit {
                    replace(R.id.container, MessagesFragment.newInstance(channel))
                    addToBackStack(null)
                }
            }
        }
    }

    private fun showLogin() {
        if (isLandscape) {
            supportFragmentManager.commit {
                replace(R.id.container_list, LoginFragment())
                replace(R.id.container_detail, SelectChatFragment())
            }
        } else {
            supportFragmentManager.commit {
                replace(R.id.container, LoginFragment())
            }
        }
    }

    private fun showChats() {
        if (isLandscape) {
            val channel = appViewModel.selectedChannel.value
            ensureLandscapeContainers(channel)
        } else {
            supportFragmentManager.commit {
                replace(R.id.container, ChatListFragment())
            }
        }
    }

    private fun ensureLandscapeContainers(channel: String?) {
        supportFragmentManager.commit {
            replace(R.id.container_list, ChatListFragment())
            if (channel != null) {
                replace(R.id.container_detail, MessagesFragment.newInstance(channel))
            } else {
                replace(R.id.container_detail, SelectChatFragment())
            }
        }
    }

    fun goToLogin() {
        TokenStore(this).clear()
        appViewModel.selectChannel(null)
        val provider = ViewModelProvider(this, vmFactory)
        provider[ChatListViewModel::class.java].reset()
        provider[MessagesViewModel::class.java].reset()
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
        showLogin()
    }

    fun goToChats() {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
        showChats()
    }

    fun openChannel(channel: String) {
        if (isLandscape) {
            supportFragmentManager.commit {
                replace(R.id.container_detail, MessagesFragment.newInstance(channel))
            }
        } else {
            supportFragmentManager.commit {
                replace(R.id.container, MessagesFragment.newInstance(channel))
                addToBackStack(null)
            }
        }
    }

    fun openImage(path: String) {
        if (isLandscape) {
            supportFragmentManager.commit {
                replace(R.id.container_detail, ImageFragment.newInstance(path))
                addToBackStack(null)
            }
        } else {
            supportFragmentManager.commit {
                replace(R.id.container, ImageFragment.newInstance(path))
                addToBackStack(null)
            }
        }
    }
}

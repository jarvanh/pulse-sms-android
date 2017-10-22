package xyz.klinker.messenger.fragment

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.ImageViewerActivity
import xyz.klinker.messenger.adapter.MediaGridAdapter
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.listener.MediaSelectedListener

class MediaGridFragment : Fragment(), MediaSelectedListener {

    private val fragmentActivity: Activity? by lazy { activity }

    val conversation: Conversation? by lazy { DataSource.getConversation(fragmentActivity!!, arguments.getLong(ARG_CONVERSATION_ID)) }
    private val messages: List<Message> by lazy { DataSource.getMediaMessages(fragmentActivity!!, conversation!!.id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpToolbar()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_media_grid, container, false)
        val recyclerView = root.findViewById<View>(R.id.recycler_view) as RecyclerView

        recyclerView.layoutManager = GridLayoutManager(fragmentActivity, resources.getInteger(R.integer.images_column_count))
        recyclerView.adapter = MediaGridAdapter(messages, this)

        return root
    }

    private fun setUpToolbar() {
        fragmentActivity?.title = conversation!!.title

        val toolbar = (fragmentActivity as AbstractSettingsActivity).toolbar
        if (Settings.useGlobalThemeColor) {
            toolbar?.setBackgroundColor(Settings.mainColorSet.color)
            ActivityUtils.setStatusBarColor(fragmentActivity, Settings.mainColorSet.colorDark)
        } else {
            toolbar?.setBackgroundColor(conversation!!.colors.color)
            ActivityUtils.setStatusBarColor(fragmentActivity, conversation!!.colors.colorDark)
        }
    }

    override fun onSelected(messageList: List<Message>, selectedPosition: Int) {
        val intent = Intent(fragmentActivity, ImageViewerActivity::class.java)
        intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, arguments.getLong(ARG_CONVERSATION_ID))
        intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageList[selectedPosition].id)
        startActivity(intent)
    }

    companion object {
        private val ARG_CONVERSATION_ID = "conversation_id"

        fun newInstance(conversationId: Long): MediaGridFragment {
            val fragment = MediaGridFragment()
            val args = Bundle()
            args.putLong(ARG_CONVERSATION_ID, conversationId)
            fragment.arguments = args

            return fragment
        }
    }
}

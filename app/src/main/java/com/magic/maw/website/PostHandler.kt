package com.magic.maw.website

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.magic.maw.data.PostData
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.YandeParser

class PostHandler(private val postList: SnapshotStateList<PostData>) : RequestHandler<PostData> {
    override val dataList: SnapshotStateList<PostData> get() = postList
    private val parser = BaseParser.getParser(YandeParser.SOURCE)
    private val requestOption = RequestOption()

    override fun refresh(force: Boolean) {
//        parser.requestPostData(option = requestOption)
    }

    override fun loadMore() {
    }
}
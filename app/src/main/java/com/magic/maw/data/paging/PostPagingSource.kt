package com.magic.maw.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.api.service.BaseApiService
import com.magic.maw.data.model.site.PostData

class PostPagingSource(
    private val apiService: BaseApiService,
    val filter: RequestFilter = RequestFilter(),
) : PagingSource<String, PostData>() {
    private val idSet = mutableSetOf<String>()

    override fun getRefreshKey(state: PagingState<String, PostData>): String? {
        // 原地刷新
//        return state.anchorPosition?.let { anchorPosition ->
//            state.closestPageToPosition(anchorPosition)?.run {
//                prevKey?.toInt()?.plus(1)?.toString()
//                    ?: nextKey?.toInt()?.minus(1)?.toString()
//            }
//        }
        // 回到顶部
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, PostData> {
        return try {
            val response = apiService.getPostData(filter, meta = RequestMeta(next = params.key))
            val items = response.items.mapNotNull { item ->
                if (idSet.contains(item.id)) null else {
                    idSet.add(item.id);item
                }
            }
            LoadResult.Page(
                data = items,
                prevKey = response.meta.prev,
                nextKey = response.meta.next
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package me.banes.chris.tivi.calls

import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import me.banes.chris.tivi.data.PopularEntry
import me.banes.chris.tivi.data.TiviShow
import me.banes.chris.tivi.data.TiviShowDao
import me.banes.chris.tivi.util.AppRxSchedulers
import me.banes.chris.tivi.util.DatabaseTxRunner
import javax.inject.Inject

class PopularCall @Inject constructor(
        databaseTxRunner: DatabaseTxRunner,
        showDao: TiviShowDao,
        tmdb: Tmdb,
        trakt: TraktV2,
        schedulers: AppRxSchedulers)
    : PaginatedTraktCall<Show>(databaseTxRunner, showDao, tmdb, trakt, schedulers) {

    override fun networkCall(page: Int): Single<List<Show>> {
        return Single.fromCallable {
            trakt.shows().popular(
                    page + 1, // Trakt uses a 1 based index
                    DEFAULT_PAGE_SIZE,
                    Extended.NOSEASONS)
                    .execute()
                    .body()
        }
    }

    override fun filterResponse(response: Show): Boolean {
        return response.ids.tmdb != null
    }

    override fun lastPageLoaded(): Single<Int> {
        return showDao.getLastPopularPage()
    }

    override fun createData(): Flowable<List<TiviShow>> {
        return showDao.popularShows()
    }

    override fun saveEntry(show: TiviShow, page: Int, order: Int) {
        val entry = PopularEntry(showId = show.id, page = page, pageOrder = order)
        showDao.insertPopularShows(entry)
    }

    override fun deleteEntries() {
        showDao.deletePopularShows()
    }

    override fun deletePage(page: Int) {
        showDao.deletePopularShowsPageSync(page)
    }

    override fun loadShow(response: Show): Maybe<TiviShow> {
        return showFromTmdb(response.ids.tmdb, response.ids.trakt)
    }

}

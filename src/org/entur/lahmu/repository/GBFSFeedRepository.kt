package org.entur.lahmu.repository

import org.entur.lahmu.domain.gbfs.v2_1.GBFSBase
import org.entur.lahmu.domain.gbfs.v2_1.GBFSFeedName

interface GBFSFeedRepository {
    fun find(systemId: String, feedName: GBFSFeedName): GBFSBase
    fun update(systemId: String, feedName: GBFSFeedName, feed: GBFSBase)
    fun load(systemId: String, feedNames: List<GBFSFeedName>, feeds: List<GBFSBase>)
}

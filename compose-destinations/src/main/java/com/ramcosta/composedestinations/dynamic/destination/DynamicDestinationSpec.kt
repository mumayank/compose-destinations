package com.ramcosta.composedestinations.dynamic.destination

import com.ramcosta.composedestinations.spec.TypedDestinationSpec

/**
 * TODO RACOSTA
 */
interface DynamicDestinationSpec<T> : TypedDestinationSpec<T> {
    val originalDestination: TypedDestinationSpec<T>
}

@PublishedApi
internal val <T> TypedDestinationSpec<T>.originalDestination
    get(): TypedDestinationSpec<T> =
        if (this is DynamicDestinationSpec<T>) {
            this.originalDestination
        } else {
            this
        }

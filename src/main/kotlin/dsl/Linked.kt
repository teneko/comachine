package de.halfbit.comachine.dsl

internal interface Linked<T : Any> {
    var next: T?
    val last: Linked<T>
        get() {
            var nextOnExit = this
            while (nextOnExit.next != null) {
                nextOnExit = nextOnExit.next as Linked<T>
            }
            return nextOnExit
        }
}
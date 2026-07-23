package ir.hrka.face.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles forward/back navigation by mutating [NavigationState].
 *
 * @property state Navigation state updated by navigation events.
 */
class Navigator(val state: NavigationState) {

    /**
     * Navigates to [destination], treating top-level keys specially.
     *
     * @param destination Target navigation key.
     */
    fun navigate(destination: NavKey) {
        when (destination) {
            state.currentTopLevelDestination -> clearSubStack()
            in state.topLevelKeys -> goToTopLevelDestination(destination)
            else -> goToSubDestination(destination)
        }
    }

    /**
     * Navigates back one step.
     *
     * @throws IllegalStateException when already at the start destination.
     */
    fun goBack() {
        when (state.currentDestination) {
            state.startDestination -> error("You cannot go back from the start route")
            state.currentTopLevelDestination -> {
                state.topLevelStack.removeLastOrNull()
            }
            else -> state.currentSubStack.removeLastOrNull()
        }
    }

    /**
     * Replaces the current top-level destination and clears its nested stack.
     *
     * Useful for splash → camera transitions where splash should leave the back stack.
     *
     * @param destination New top-level destination.
     */
    fun replaceTopLevel(destination: NavKey) {
        state.topLevelStack.apply {
            clear()
            add(destination)
        }
        state.subStacks[destination]?.apply {
            clear()
            add(destination)
        }
    }

    private fun goToSubDestination(destination: NavKey) {
        state.currentSubStack.apply {
            remove(destination)
            add(destination)
        }
    }

    private fun goToTopLevelDestination(destination: NavKey) {
        state.topLevelStack.apply {
            if (destination == state.startDestination) {
                clear()
            } else {
                remove(destination)
            }
            add(destination)
        }
    }

    private fun clearSubStack() {
        state.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}

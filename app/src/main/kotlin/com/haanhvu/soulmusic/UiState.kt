package com.haanhvu.soulmusic

/**
 * A sealed hierarchy describing the state of the text generation.
 */
sealed interface UiState {

    /**
     * Empty state when the screen is first shown
     */
    object Initial : UiState

    /**
     * Still loading
     */
    object Loading : UiState

    /**
     * Data has been generated
     */
    data class Success(val output: Map<String, String>) : UiState

    /**
     * There was an error generating data
     */
    data class Error(val errorMessage: String) : UiState
}
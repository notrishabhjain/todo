package com.procrastinationkiller.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.dao.KeywordDao
import com.procrastinationkiller.data.local.entity.KeywordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeywordManagementUiState(
    val allKeywords: List<KeywordEntity> = emptyList(),
    val filteredKeywords: List<KeywordEntity> = emptyList(),
    val selectedCategory: String = "All",
    val isLoading: Boolean = true
)

@HiltViewModel
class KeywordManagementViewModel @Inject constructor(
    private val keywordDao: KeywordDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeywordManagementUiState())
    val uiState: StateFlow<KeywordManagementUiState> = _uiState.asStateFlow()

    init {
        loadKeywords()
    }

    private fun loadKeywords() {
        viewModelScope.launch {
            keywordDao.getAllKeywords().collect { keywords ->
                val filtered = filterKeywords(keywords, _uiState.value.selectedCategory)
                _uiState.value = _uiState.value.copy(
                    allKeywords = keywords,
                    filteredKeywords = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun selectCategory(category: String) {
        val filtered = filterKeywords(_uiState.value.allKeywords, category)
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            filteredKeywords = filtered
        )
    }

    fun addKeyword(keywordText: String, category: String) {
        viewModelScope.launch {
            val entity = KeywordEntity(
                keyword = keywordText,
                category = category,
                weight = 1.0f,
                isUserDefined = true
            )
            keywordDao.insertKeyword(entity)
        }
    }

    fun deleteKeyword(keyword: KeywordEntity) {
        viewModelScope.launch {
            keywordDao.deleteKeyword(keyword)
        }
    }

    private fun filterKeywords(keywords: List<KeywordEntity>, category: String): List<KeywordEntity> {
        return if (category == "All") {
            keywords
        } else {
            keywords.filter { it.category == category }
        }
    }
}

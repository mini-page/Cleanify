# Contributing to Cleanify

Thank you for your interest in contributing to Cleanify! We're excited to have your help in making the app better. This document outlines our guidelines to ensure a smooth and effective collaboration process for everyone.

## How to Contribute

The best way to contribute is by getting involved in the discussion. All feature proposals and bug reports should start as a GitHub Issue.

-   **Found a Bug?** [Open a Bug Report](https://github.com/LoopOtto/Cleanify/issues/new?template=bug_report.md&title=%5BBUG%5D%20). Please be as detailed as possible, including your device model, Android version, and steps to reproduce the issue.
-   **Have a Feature Idea?** [Propose a New Feature](https://github.com/LoopOtto/Cleanify/issues/new?template=feature_request.md&title=%5BFEAT%5D%20). Explain the problem you're trying to solve and how your proposed feature would address it.

Please search the existing issues before creating a new one to avoid duplicates.

## Our Development Process

To maintain project quality and clarity, we follow a structured development process:

1.  **Proposal (Issue):** Every change starts with a GitHub Issue. This is where the problem or feature is defined and discussed. This step prevents wasted effort on a change that might not align with the project's goals.
2.  **Discussion & Approval:** The project maintainers will review the issue. For new features, when needed, we will discuss the proposed solution and agree on a high-level plan before any code is written.
3.  **Implementation (Pull Request):** Once the plan is approved, you can begin implementation. Create a Pull Request (PR) and link it to the original issue. Please keep PRs focused on a single issue.

## Architectural & Coding Standards

To ensure the codebase remains clean, stable, and maintainable, all contributions must adhere to our core architectural principles.

### 1. MVVM & Unidirectional Data Flow (UDF)

The app is built on a Model-View-ViewModel (MVVM) architecture with a strict UDF pattern. State should flow down from the ViewModel to the UI, and events should flow up from the UI to the ViewModel.

### 2. Core Composable Design: State Decoupling

This is a critical, non-negotiable principle for all UI code. To ensure components are reusable, testable, and have clear API contracts, we **must avoid** passing entire `UiState` objects to child composables.

Instead, a composable function must **only accept the specific parameters it actually needs.**

**Rationale:**

-   **Reusability & Previewability:** A component that accepts simple types (`String`, `Boolean`, `() -> Unit`) is trivial to reuse anywhere and to preview in Android Studio.
-   **API Clarity:** The function's signature becomes a perfect, self-documenting contract of its dependencies.
-   **Encapsulation:** It prevents a child component from accessing or depending on state it has no business knowing about.

**Example:**


#### INCORRECT (Violates the principle)

```kotlin
// This component is fragile and hard to reuse because it depends on the entire state.
@Composable
fun MyComponent(uiState: SwiperUiState) {
    Text(uiState.currentItem.displayName)
    Button(enabled = !uiState.isLoading) { /* ... */ }
}
```

#### CORRECT (Adheres to the principle)

```kotlin
// This component has a clear, reusable, and protected API.
@Composable
fun MyComponent(
    displayName: String,
    isLoading: Boolean,
    onButtonClicked: () -> Unit
) {
    Text(displayName)
    Button(enabled = !isLoading, onClick = onButtonClicked) { /* ... */ }
}
```

## Submitting a Pull Request

When your changes are ready, please submit a Pull Request with the following:

-   A clear title that summarizes the change (e.g., `[FEAT] Add folder exclusion to duplicate scan`).
-   A description explaining *what* the change is and *why* it's being made.
-   A link to the GitHub Issue it resolves (e.g., `Closes #123`).
-   Ensure your code is formatted according to the project's existing style.

Thank you for helping make Cleanify a better app!
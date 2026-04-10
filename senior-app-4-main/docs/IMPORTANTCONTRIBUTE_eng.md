# Contributing to S.P.M.S.

First of all, thank you for contributing to the "Senior Project Management System" project <3. This document is prepared to standardize our development processes within the team, improve our code quality, and ensure a smooth Review process.

Please read these guidelines carefully before writing any code or opening a Pull Request (PR).

## 1. Branch Strategy

To maintain a clean and understandable Git history in our project, we use the following naming standards:

* **New Features:** `feature/short-feature-name` (e.g., `feature/github-oauth`, `feature/professor-login`)
* **Bug Fixes:** `bugfix/short-bug-name` (e.g., `bugfix/jwt-token-error`)
* **Documentation:** `docs/updated-document` (e.g., `docs/api-yaml-update`)
* **Hotfixes:** `hotfix/critical-error` (Only for emergencies occurring on the main branch)

Please do not push code directly to the `main` or `develop` branches.

## 2. Commit Message Standards

We adopt the [Conventional Commits](https://www.conventionalcommits.org/) standard so that our commit messages can be easily read by our AI integration and advisor reviews:

* `feat:` When a new feature is added (e.g., `feat: add professor login page`)
* `fix:` When a bug is resolved (e.g., `fix: resolve db connection timeout`)
* `docs:` When only documentation is changed (e.g., `docs: update setup instructions`)
* `refactor:` Code changes that neither fix a bug nor add a feature
* `style:` Formatting changes that do not affect the execution of the code (white-space, missing semi-colons, etc.)

## 3. Pull Request (PR) and Review Process

This process is of critical importance as our system's grading mechanism (Evaluation Rubric) and AI analysis tools work integrated with PR comments.

1. Once you are done with your branch, open a Pull Request to the `main` (or `develop`) branch.
2. Your PR title should be clear and include the related GitHub Issue number (e.g., `feat: add initial password change form (Resolves #4)`).
3. Briefly explain what you changed and how it can be tested in the PR description.
4. Contact your team leader as the **Reviewer**.
5. A PR cannot be merged without reviewer approval and passing all automated tests/checks.

## 4. Local Setup

Follow the steps below to run the project on your local machine:

1. Clone the repo: `git clone <repo-url>`
2. Install dependencies: `npm install` (or the corresponding command for the backend)
3. Set up environment variables: Copy the `.env.example` file to `.env` and fill in the required values.
4. Start the development server: `npm run dev`

If you have any questions, please reach out to the project team leader.
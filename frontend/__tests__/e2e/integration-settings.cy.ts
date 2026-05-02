/* TDD SPECIFICATION: E2E Automation - UI Integration Settings & Toasts
   Issue #5 (Covers #17, #20, #18, #19, #24)
   Uncomment and implement when the Integration Settings UI is ready.

describe('Integration Settings & Toast E2E Tests', () => {
  beforeEach(() => {
    // Intercept the API call to mock a successful settings update
    cy.intercept('POST', '/api/v1/integrations/settings', {
      statusCode: 200,
      body: { message: 'Settings saved successfully' }
    }).as('saveSettings');

    // Intercept the API call to mock the live status
    cy.intercept('GET', '/api/v1/integrations/status', {
      statusCode: 200,
      body: { github: 'CONNECTED', jira: 'DISCONNECTED' }
    }).as('getStatus');

    cy.visit('/settings/integrations');
  });

  it('masks the GitHub PAT input field automatically (Issue #17)', () => {
    // Assert via DOM manipulation that the type of the PAT input element is 'password'
    cy.get('input[name="githubPat"]').should('have.attr', 'type', 'password');
    
    // Type into it and ensure it works
    cy.get('input[name="githubPat"]').type('ghp_mock_token_123');
    cy.get('input[name="githubPat"]').should('have.value', 'ghp_mock_token_123');
  });

  it('triggers HTML/Zod validation when JIRA URL is empty (Issue #17)', () => {
    // Fill GitHub but leave JIRA empty
    cy.get('input[name="githubPat"]').type('ghp_mock_token_123');
    cy.get('input[name="jiraSpaceUrl"]').clear();

    cy.get('button[type="submit"]').click();

    // Verify validation warning appears in the DOM
    cy.contains('JIRA Space URL is required').should('be.visible');
  });

  it('displays a Global Success Toast on valid submission (Issue #5, #20, #24)', () => {
    // Fill all fields validly
    cy.get('input[name="githubPat"]').type('ghp_mock_token_123');
    cy.get('input[name="jiraSpaceUrl"]').type('https://mycompany.atlassian.net');

    cy.get('button[type="submit"]').click();
    cy.wait('@saveSettings');

    // Assert that a green Toast notification with a 'Success' class appears
    cy.get('.toast-success') // Or whatever Sonner/react-toastify uses
      .should('be.visible')
      .and('contain', 'Settings saved successfully');
  });

  it('displays Live Integration Status Indicators correctly (Issue #18)', () => {
    cy.wait('@getStatus');

    // GitHub should show a green connected dot
    cy.get('[data-testid="status-github"]')
      .should('have.class', 'bg-green-500')
      .and('contain', 'CONNECTED');

    // JIRA should show a red disconnected dot
    cy.get('[data-testid="status-jira"]')
      .should('have.class', 'bg-red-500')
      .and('contain', 'DISCONNECTED');
  });

  it('triggers Sync Now and displays loading state (Issue #19)', () => {
    cy.intercept('POST', '/api/v1/scrum-sync/trigger', {
      statusCode: 202,
      body: { status: 'Sync started' }
    }).as('triggerSync');

    cy.contains('button', 'Sync Now').click();

    // Verify button shows loading state (spinner or text change)
    cy.contains('button', 'Syncing...').should('be.visible');
    cy.get('.lucide-loader').should('be.visible'); // Assuming Lucide spinner

    cy.wait('@triggerSync');
  });
  it('displays a Global Error Toast on 500 API failure (Issue #24)', () => {
    // Intercept with a 500 Server Error to test the Global Axios Interceptor
    cy.intercept('POST', '/api/v1/integrations/settings', {
      statusCode: 500,
      body: { message: 'Internal Server Error' }
    }).as('saveSettingsError');

    cy.get('input[name="githubPat"]').type('ghp_mock_token_123');
    cy.get('input[name="jiraSpaceUrl"]').type('https://mycompany.atlassian.net');

    cy.get('button[type="submit"]').click();
    cy.wait('@saveSettingsError');

    // Assert that a red Toast notification with an 'Error' class appears
    cy.get('.toast-error')
      .should('be.visible')
      .and('contain', 'Internal Server Error');
  });
});
*/

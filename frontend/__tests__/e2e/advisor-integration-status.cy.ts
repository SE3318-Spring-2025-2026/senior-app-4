/* E2E SPECIFICATION: Advisor Dashboard — Integration Status Indicators
   Issue: JIRA & GitHub Connectivity Visual Indicator on Advisor Dashboard
   Covers: Acceptance Criteria (green/red badge, connectedAt timestamp, skeleton loading)
   Route: /professor/my-advisees
*/

describe('Advisor Dashboard — Integration Status Indicators', () => {

    const GROUP_ID = 1;

    const MOCK_ASSIGNMENT = {
        teamId: GROUP_ID,
        teamName: 'Team Alpha',
        leaderName: 'Ali Yılmaz',
        advisorId: 99,
        advisorName: 'Prof. Test',
        status: 'ADVISED',
        assignedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
        assignmentType: 'STANDARD',
    };

    beforeEach(() => {
        // Auth localStorage keys must match spms_token / spms_user
        cy.window().then((win) => {
            win.localStorage.setItem('spms_token', 'mock-professor-token');
            win.localStorage.setItem('spms_user', JSON.stringify({
                role: 'professor',
                requiresPasswordChange: false,
            }));
        });

        // fetchAdvisorAssignments expects { status, data: [...] }
        cy.intercept('GET', '/api/v1/advisor-assignments*', {
            statusCode: 200,
            body: {
                status: 'ok',
                data: [MOCK_ASSIGNMENT],
            },
        }).as('getAdvisees');
    });

    describe('Skeleton Loading State', () => {
        it('shows skeleton elements while integration status is loading', () => {
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/github`, (req) => {
                req.reply({
                    delay: 1500,
                    statusCode: 200,
                    body: { success: true, data: { status: 'active', organizationName: 'org', connectedAt: new Date().toISOString(), message: null } },
                });
            }).as('getGithubSlow');

            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/jira`, (req) => {
                req.reply({
                    delay: 1500,
                    statusCode: 200,
                    body: { success: true, data: { status: 'active', jiraSpaceUrl: 'https://team.atlassian.net', projectKey: 'TEAM', connectedAt: new Date().toISOString(), message: null } },
                });
            }).as('getJiraSlow');

            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');

            cy.get(`[data-testid="integration-github-group-${GROUP_ID}-skeleton"]`).should('exist');
            cy.get(`[data-testid="integration-jira-group-${GROUP_ID}-skeleton"]`).should('exist');
        });
    });

    describe('Connected State (green badge)', () => {
        beforeEach(() => {
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/github`, {
                statusCode: 200,
                body: {
                    success: true,
                    data: {
                        status: 'active',
                        organizationName: 'my-org',
                        connectedAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
                        message: null,
                    },
                },
            }).as('getGithub');

            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/jira`, {
                statusCode: 200,
                body: {
                    success: true,
                    data: {
                        status: 'active',
                        jiraSpaceUrl: 'https://team.atlassian.net',
                        projectKey: 'TEAM',
                        connectedAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
                        message: null,
                    },
                },
            }).as('getJira');
        });

        it('shows green Connected badge for active GitHub integration', () => {
            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getGithub');

            cy.get(`[data-testid="integration-github-group-${GROUP_ID}"]`)
                .should('be.visible')
                .and('have.class', 'bg-green-500/10')
                .and('contain.text', 'GitHub — Connected');
        });

        it('shows green Connected badge for active JIRA integration', () => {
            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getJira');

            cy.get(`[data-testid="integration-jira-group-${GROUP_ID}"]`)
                .should('be.visible')
                .and('have.class', 'bg-green-500/10')
                .and('contain.text', 'JIRA — Connected');
        });

        it('shows connectedAt timestamp inside the Connected badge', () => {
            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getGithub');

            cy.get(`[data-testid="integration-github-group-${GROUP_ID}"]`)
                .should('contain.text', 'Connected At:');
        });
    });

    describe('Disconnected State (red badge)', () => {
        beforeEach(() => {
            // GitHub not bound — fetchGithubIntegration handles 404 internally and returns inactive
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/github`, {
                statusCode: 404,
                body: { message: 'No GitHub integration exists for this group.' },
            }).as('getGithubNotFound');

            // JIRA not bound — returns inactive shape
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/jira`, {
                statusCode: 200,
                body: {
                    success: true,
                    data: {
                        status: 'inactive',
                        jiraSpaceUrl: null,
                        projectKey: null,
                        connectedAt: null,
                        message: 'Not connected',
                    },
                },
            }).as('getJiraInactive');
        });

        it('shows red No Connection badge when GitHub has no integration', () => {
            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getGithubNotFound');

            cy.get(`[data-testid="integration-github-group-${GROUP_ID}"]`)
                .should('be.visible')
                .and('have.class', 'bg-red-500/10')
                .and('contain.text', 'GitHub — No Connection');
        });

        it('shows red No Connection badge when JIRA status is inactive', () => {
            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getJiraInactive');

            cy.get(`[data-testid="integration-jira-group-${GROUP_ID}"]`)
                .should('be.visible')
                .and('have.class', 'bg-red-500/10')
                .and('contain.text', 'JIRA — No Connection');
        });

        it('shows "Never connected" when connectedAt is null', () => {
            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getJiraInactive');

            cy.get(`[data-testid="integration-jira-group-${GROUP_ID}"]`)
                .should('contain.text', 'Never connected');
        });
    });

    describe('Both integrations shown per row', () => {
        it('renders both GitHub and JIRA indicators in the same row', () => {
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/github`, {
                statusCode: 200,
                body: { success: true, data: { status: 'active', organizationName: 'org', connectedAt: new Date().toISOString(), message: null } },
            }).as('getGithub');

            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/jira`, {
                statusCode: 200,
                body: { success: true, data: { status: 'inactive', jiraSpaceUrl: null, projectKey: null, connectedAt: null, message: null } },
            }).as('getJira');

            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');
            cy.wait('@getGithub');
            cy.wait('@getJira');

            cy.get(`[data-testid="integration-github-group-${GROUP_ID}"]`).should('exist');
            cy.get(`[data-testid="integration-jira-group-${GROUP_ID}"]`).should('exist');
        });
    });

    describe('Integrations table column', () => {
        it('renders the Integrations column header', () => {
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/github`, {
                statusCode: 404,
                body: {},
            });
            cy.intercept('GET', `/api/v1/groups/${GROUP_ID}/integrations/jira`, {
                statusCode: 200,
                body: { success: true, data: { status: 'inactive', jiraSpaceUrl: null, projectKey: null, connectedAt: null, message: null } },
            });

            cy.visit('/professor/my-advisees');
            cy.wait('@getAdvisees');

            cy.contains('th', 'Integrations').should('be.visible');
        });
    });
});

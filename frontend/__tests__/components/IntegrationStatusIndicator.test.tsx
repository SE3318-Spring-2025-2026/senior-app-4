/* TDD SPECIFICATION: IntegrationStatusIndicator Component
   Issue: Advisor Dashboard — JIRA & GitHub Integration Status Indicator
   Covers: Acceptance Criteria (connected/disconnected badge, connectedAt timestamp, skeleton loading)
*/

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import IntegrationStatusIndicator from '@/components/IntegrationStatusIndicator';

describe('IntegrationStatusIndicator', () => {

    describe('Skeleton Loading State', () => {
        it('renders skeleton element when loading=true', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={false}
                    connectedAt={null}
                    loading={true}
                    data-testid="github-status"
                />
            );
            expect(screen.getByTestId('github-status-skeleton')).toBeInTheDocument();
        });

        it('does not render label text when loading=true', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={false}
                    connectedAt={null}
                    loading={true}
                />
            );
            expect(screen.queryByText(/GitHub/)).not.toBeInTheDocument();
        });

        it('skeleton element has animate-pulse class', () => {
            render(
                <IntegrationStatusIndicator
                    label="JIRA"
                    connected={false}
                    connectedAt={null}
                    loading={true}
                    data-testid="jira-status"
                />
            );
            expect(screen.getByTestId('jira-status-skeleton')).toHaveClass('animate-pulse');
        });
    });

    describe('Connected State (green badge)', () => {
        it('renders green bg when connected=true', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={null}
                    data-testid="github-status"
                />
            );
            expect(screen.getByTestId('github-status')).toHaveClass('bg-green-500/10');
        });

        it('shows "Connected" label text when connected=true', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={null}
                />
            );
            expect(screen.getByText('GitHub — Connected')).toBeInTheDocument();
        });

        it('does not render red styling when connected=true', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={null}
                    data-testid="github-status"
                />
            );
            expect(screen.getByTestId('github-status')).not.toHaveClass('bg-red-500/10');
        });
    });

    describe('Disconnected State (red badge)', () => {
        it('renders red bg when connected=false', () => {
            render(
                <IntegrationStatusIndicator
                    label="JIRA"
                    connected={false}
                    connectedAt={null}
                    data-testid="jira-status"
                />
            );
            expect(screen.getByTestId('jira-status')).toHaveClass('bg-red-500/10');
        });

        it('shows "No Connection" label text when connected=false', () => {
            render(
                <IntegrationStatusIndicator
                    label="JIRA"
                    connected={false}
                    connectedAt={null}
                />
            );
            expect(screen.getByText('JIRA — No Connection')).toBeInTheDocument();
        });

        it('does not render green styling when connected=false', () => {
            render(
                <IntegrationStatusIndicator
                    label="JIRA"
                    connected={false}
                    connectedAt={null}
                    data-testid="jira-status"
                />
            );
            expect(screen.getByTestId('jira-status')).not.toHaveClass('bg-green-500/10');
        });
    });

    describe('connectedAt Timestamp', () => {
        it('shows "Never connected" when connectedAt is null', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={false}
                    connectedAt={null}
                />
            );
            expect(screen.getByText('Never connected')).toBeInTheDocument();
        });

        it('shows "just now" for very recent timestamps', () => {
            const recent = new Date(Date.now() - 10_000).toISOString();
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={recent}
                />
            );
            expect(screen.getByText('Connected At: just now')).toBeInTheDocument();
        });

        it('shows minutes ago for timestamps under an hour', () => {
            const tenMinsAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString();
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={tenMinsAgo}
                />
            );
            expect(screen.getByText('Connected At: 10 mins ago')).toBeInTheDocument();
        });

        it('shows hours ago for timestamps under a day', () => {
            const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();
            render(
                <IntegrationStatusIndicator
                    label="JIRA"
                    connected={true}
                    connectedAt={threeHoursAgo}
                />
            );
            expect(screen.getByText('Connected At: 3h ago')).toBeInTheDocument();
        });

        it('shows days ago for timestamps under a month', () => {
            const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString();
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={twoDaysAgo}
                />
            );
            expect(screen.getByText('Connected At: 2d ago')).toBeInTheDocument();
        });
    });

    describe('data-testid propagation', () => {
        it('attaches data-testid to the root element when not loading', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={true}
                    connectedAt={null}
                    data-testid="github-status"
                />
            );
            expect(screen.getByTestId('github-status')).toBeInTheDocument();
        });

        it('attaches data-testid with -skeleton suffix when loading', () => {
            render(
                <IntegrationStatusIndicator
                    label="GitHub"
                    connected={false}
                    connectedAt={null}
                    loading={true}
                    data-testid="github-status"
                />
            );
            expect(screen.getByTestId('github-status-skeleton')).toBeInTheDocument();
            expect(screen.queryByTestId('github-status')).not.toBeInTheDocument();
        });
    });
});

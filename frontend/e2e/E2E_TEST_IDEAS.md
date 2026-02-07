# E2E Test Ideas

This file tracks ideas for end-to-end tests that should be implemented in the future.

## Chart Rendering Tests

Testing actual chart rendering is better suited for E2E tests rather than unit tests because:
- Unit tests use jsdom which doesn't fully support all DOM APIs
- Chart libraries (like Unovis) rely on browser-specific features
- E2E tests run in a real browser environment

### Ideas:
- [ ] Verify SpendingChart renders with correct data points
- [ ] Verify CategoryChart (DonutChart) displays all categories
- [ ] Test chart tooltips appear on hover
- [ ] Test chart responsiveness on different screen sizes
- [ ] Verify chart animations complete without errors

## Other E2E Test Ideas

(Add future E2E test ideas here)

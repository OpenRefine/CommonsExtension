describe(__filename, function () {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Test the create project from a Commons category', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > a')
      .contains('Wikimedia Commons')
      .click();
    // enter a category name
    cy.get(
      'input.category-input-box'
    ).type('Official OpenRefine logos');
    cy.get(
      '.fbs-item-name'
    )
      .contains('Official OpenRefine logos')
      .click();

    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    // preview and click next
    cy.get('button[bind="createProjectButton"]')
      .contains('Create Project »')
      .click();
    cy.waitForProjectTable();
  });
});

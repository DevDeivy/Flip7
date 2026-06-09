Feature: Multiplayer room flow using real backend
  As a player
  I want to create a multiplayer room from the UI
  So that I can start a real shared match

  Scenario: Host creates a room and sees lobby data
    Given I open the home page
    When I go to multiplayer mode
    And I create a room with my host name
    Then I should see the lobby with a room code
    And I should see myself as host in the room

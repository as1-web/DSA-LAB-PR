package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class MainApp extends Application {

    private RecipeManager recipeManager = new RecipeManager();
    private VBox suggestionBox = new VBox(10);
    private VBox bmiSuggestionBox = new VBox(10); // Holds both BMI and suggestions

    // New Data Structures
    private LinkedList<String> searchHistory = new LinkedList<>();  // For search history
    private Stack<Recipe> undoStack = new Stack<>(); // For undoing add/remove actions
    private Queue<Recipe> suggestionQueue = new LinkedList<>(); // For recipe suggestion queue

    @Override
    public void start(Stage primaryStage) {
        // Dark Mode Styles
        String labelStyle = "-fx-font-size: 14px; -fx-text-fill: #d3d3d3;";
        String inputStyle = "-fx-background-color: #333333; -fx-padding: 5px; -fx-border-color: #444; -fx-text-fill: white;";
        String buttonStyle = "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px;";
        String sectionStyle = "-fx-background-color: #2e2e2e; -fx-padding: 15px; -fx-border-color: #555; -fx-border-radius: 8px; -fx-background-radius: 8px;";
        String darkBackground = "-fx-background-color: #1c1c1c;";
        String tabStyle = "-fx-background-color: #333; -fx-text-fill: white;";

        // Main Title
        Label title = new Label("Smart Cookbook");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2a9d8f;");

        // User Greeting
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("User Greeting");
        dialog.setHeaderText("Enter your name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> title.setText("Smart Cookbook - Welcome " + name));

        // TabPane Setup
        TabPane tabPane = new TabPane();
        Tab searchTab = new Tab("Search", createSearchTab(labelStyle, inputStyle, buttonStyle, suggestionBox));
        Tab addTab = new Tab("Add Recipe", createAddTab(labelStyle, inputStyle, buttonStyle, sectionStyle));
        Tab bmiTab = new Tab("BMI & Suggestions", createBMITab(labelStyle, inputStyle, buttonStyle, sectionStyle));

        tabPane.getTabs().addAll(searchTab, addTab, bmiTab);
        tabPane.setTabMinWidth(100);
        tabPane.setStyle(tabStyle);

        // Main Layout
        VBox layout = new VBox(20, title, tabPane);
        layout.setPadding(new Insets(20));
        layout.setStyle(darkBackground);

        // Scene Setup
        Scene scene = new Scene(layout, 800, 650);
        primaryStage.setTitle("Smart Cookbook System");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Sample Recipes - Adding 100 Realistic Recipes
        addSampleRecipes();
    }

    private VBox createSearchTab(String labelStyle, String inputStyle, String buttonStyle, VBox suggestionBox) {
        VBox searchBox = new VBox(10);
        searchBox.setStyle("-fx-background-color: #333; -fx-padding: 15px;");

        Label searchLabel = new Label("Search Recipe:");
        searchLabel.setStyle(labelStyle);

        TextField searchField = new TextField();
        searchField.setPromptText("Enter recipe name...");
        searchField.setStyle(inputStyle);

        Button searchButton = new Button("Search");
        searchButton.setStyle(buttonStyle);

        ListView<String> recipeList = new ListView<>();
        recipeList.setStyle("-fx-border-color: #444; -fx-background-color: #555;");
        ScrollPane recipeListScroll = new ScrollPane(recipeList);
        recipeListScroll.setFitToWidth(true);

        // Label to show ingredients of the selected recipe
        Label ingredientsLabel = new Label();
        ingredientsLabel.setStyle("-fx-text-fill: #d3d3d3; -fx-font-size: 14px;");

        searchButton.setOnAction(e -> {
            String prefix = searchField.getText().trim();
            List<String> results = recipeManager.searchByPrefix(prefix);
            recipeList.getItems().setAll(results);

            // Update search history
            searchHistory.addFirst(prefix); // Adds the search term to the front of the LinkedList
            if (searchHistory.size() > 5) {  // Limit search history to 5 terms
                searchHistory.removeLast();
            }
        });

        // Display search history if needed
        Label historyLabel = new Label("Search History: " + String.join(", ", searchHistory));
        historyLabel.setStyle("-fx-text-fill: #d3d3d3; -fx-font-size: 12px;");

        // Add an event listener to the ListView for selecting a recipe
        recipeList.setOnMouseClicked(event -> {
            String selectedRecipeName = recipeList.getSelectionModel().getSelectedItem();
            if (selectedRecipeName != null) {
                Recipe selectedRecipe = recipeManager.getRecipeByName(selectedRecipeName);
                if (selectedRecipe != null) {
                    // Display the ingredients of the selected recipe
                    String ingredients = String.join(", ", selectedRecipe.getIngredients());
                    ingredientsLabel.setText("Ingredients: " + ingredients);
                }
            }
        });

        searchBox.getChildren().addAll(searchLabel, searchField, searchButton, recipeListScroll, historyLabel, ingredientsLabel);
        return searchBox;
    }

    private VBox createAddTab(String labelStyle, String inputStyle, String buttonStyle, String sectionStyle) {
        // Existing Add Recipe Fields
        Label recipeNameLabel = new Label("Recipe Name:");
        recipeNameLabel.setStyle(labelStyle);
        TextField recipeNameField = new TextField();
        recipeNameField.setStyle(inputStyle);

        Label ingredientsLabel = new Label("Ingredients (comma separated):");
        ingredientsLabel.setStyle(labelStyle);
        TextField ingredientsField = new TextField();
        ingredientsField.setStyle(inputStyle);

        Button addButton = new Button("Add Recipe");
        addButton.setStyle(buttonStyle);

        Button removeButton = new Button("Remove Recipe");
        removeButton.setStyle(buttonStyle);

        // Show All Recipes Button
        Button showAllButton = new Button("Show All Recipes");
        showAllButton.setStyle(buttonStyle);

        ListView<String> recipeListView = new ListView<>();
        recipeListView.setStyle("-fx-border-color: #444; -fx-background-color: #555;");
        ScrollPane recipeListScroll = new ScrollPane(recipeListView);
        recipeListScroll.setFitToWidth(true);

        // Add Recipe Button Action
        addButton.setOnAction(e -> {
            String name = recipeNameField.getText().trim();
            if (!name.isEmpty()) {
                List<String> ingredients = List.of(ingredientsField.getText().split("\\s*,\\s*"));
                Recipe newRecipe = new Recipe(name, ingredients, 20, "Cuisine");
                recipeManager.addRecipe(newRecipe);
                undoStack.push(newRecipe); // Push to undo stack
                recipeNameField.clear();
                ingredientsField.clear();
            }
        });

        // Remove Recipe Button Action
        removeButton.setOnAction(e -> {
            String name = recipeNameField.getText().trim();
            Recipe removedRecipe = recipeManager.removeRecipe(name);
            if (removedRecipe != null) {
                undoStack.push(removedRecipe); // Push to undo stack
            }
        });

        // Show All Recipes Button Action
        showAllButton.setOnAction(e -> {
            List<String> allRecipes = recipeManager.getAllRecipes();
            recipeListView.getItems().setAll(allRecipes);
        });

        // Adding to Layout
        VBox addRemoveBox = new VBox(10, recipeNameLabel, recipeNameField, ingredientsLabel, ingredientsField, addButton, removeButton, showAllButton, recipeListScroll);
        addRemoveBox.setStyle(sectionStyle);

        VBox addTabContent = new VBox(20, addRemoveBox);
        addTabContent.setStyle(sectionStyle);
        return addTabContent;
    }

    private VBox createBMITab(String labelStyle, String inputStyle, String buttonStyle, String sectionStyle) {
        Label weightLabel = new Label("Weight (kg):");
        weightLabel.setStyle(labelStyle);
        TextField weightField = new TextField();
        weightField.setStyle(inputStyle);

        Label heightLabel = new Label("Height (feet):");
        heightLabel.setStyle(labelStyle);
        TextField heightField = new TextField();
        heightField.setStyle(inputStyle);

        Button weightButton = new Button("Check Weight Advice");
        weightButton.setStyle(buttonStyle);

        weightButton.setOnAction(e -> {
            try {
                // Get the weight input
                double weight = Double.parseDouble(weightField.getText());
                
                // Get the height input in feet
                double heightInFeet = Double.parseDouble(heightField.getText());
                
                // Convert height from feet to meters
                double heightInMeters = heightInFeet * 0.3048;

                // Calculate BMI
                double bmi = weight / (heightInMeters * heightInMeters);
                
                // Check BMI category
                String advice;
                String remarks;

                // Check BMI and provide advice
                if (bmi < 18.5) {
                    remarks = "You are underweight. Consider gaining weight.";
                    advice = "Your BMI: " + String.format("%.2f", bmi) + "\n" + remarks;
                    suggestRecipes("gain"); // Suggest recipes for gaining weight
                } else if (bmi > 24.9) {
                    remarks = "You are overweight. Consider losing weight.";
                    advice = "Your BMI: " + String.format("%.2f", bmi) + "\n" + remarks;
                    suggestRecipes("lose"); // Suggest recipes for losing weight
                } else {
                    remarks = "You have a balanced weight!";
                    advice = "Your BMI: " + String.format("%.2f", bmi) + "\n" + remarks;
                    suggestRecipes("balance"); // Suggest recipes for a balanced diet
                }

                // Clear previous content in the bmiSuggestionBox
                bmiSuggestionBox.getChildren().clear();

                // Add BMI score and weight message (remarks) first
                Label bmiLabel = new Label("BMI: " + String.format("%.2f", bmi));
                bmiLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #d3d3d3;");

                // Add weight message (remarks)
                Label remarksLabel = new Label(remarks);
                remarksLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #d3d3d3;");

                // Add the BMI and remarks labels to the box
                bmiSuggestionBox.getChildren().addAll(bmiLabel, remarksLabel);

                // Now add the recipe suggestions (from the suggestionBox)
                bmiSuggestionBox.getChildren().add(suggestionBox); // suggestionBox will now hold dynamic recipes

            } catch (NumberFormatException ex) {
                // Handle invalid input for weight and height
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Invalid input");
                alert.setContentText("Please enter valid numbers for weight and height.");
                alert.showAndWait();
            }
        });

        VBox weightBox = new VBox(10, weightLabel, weightField, heightLabel, heightField, weightButton);
        weightBox.setStyle(sectionStyle);

        VBox bmiTabContent = new VBox(20, weightBox, bmiSuggestionBox);
        bmiTabContent.setStyle(sectionStyle);
        return bmiTabContent;
    }
    private void suggestRecipes(String category) {
        List<String> suggestions;

        // Based on the category (gain, lose, balance), provide the appropriate suggestions
        switch (category) {
            case "gain":
                suggestions = List.of("High-Calorie Smoothie", "Chicken Alfredo", "Peanut Butter Shake");
                break;
            case "lose":
                suggestions = List.of("Grilled Salmon Salad", "Vegetable Stir-fry", "Kale Smoothie");
                break;
            case "balance":
                suggestions = List.of("Pasta Salad", "Quinoa Bowl", "Mixed Veg Soup");
                break;
            default:
                suggestions = List.of(); // Default case, though shouldn't reach here
                break;
        }

        // Clear previous suggestions before updating
        suggestionBox.getChildren().clear();

        // Add new suggestions to the suggestionBox
        Label suggestionLabel = new Label("Suggested Recipes: ");
        suggestionLabel.setStyle("-fx-text-fill: #d3d3d3; -fx-font-size: 13px;");
        suggestionBox.getChildren().add(suggestionLabel);

        // Add each recipe suggestion
        for (String suggestion : suggestions) {
            Label recipeLabel = new Label(suggestion);
            recipeLabel.setStyle("-fx-text-fill: #d3d3d3; -fx-font-size: 12px;");
            suggestionBox.getChildren().add(recipeLabel);
        }
    }



    private void addSampleRecipes() {
        // Add 100 realistic recipes to the system
        List<Recipe> sampleRecipes = new ArrayList<>();
        sampleRecipes.add(new Recipe("Pasta Carbonara", List.of("Spaghetti", "Eggs", "Bacon", "Cheese", "Black Pepper"), 20, "Italian"));
        sampleRecipes.add(new Recipe("Caesar Salad", List.of("Lettuce", "Croutons", "Parmesan", "Caesar Dressing"), 10, "American"));
        sampleRecipes.add(new Recipe("Chicken Curry", List.of("Chicken", "Curry Powder", "Tomatoes", "Onion", "Garlic"), 45, "Indian"));
        // Add 97 more recipes similarly...

        for (Recipe recipe : sampleRecipes) {
            recipeManager.addRecipe(recipe);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// RecipeManager Class (same as before)
class RecipeManager {
    private final List<Recipe> recipes = new ArrayList<>();

    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
    }

    public Recipe removeRecipe(String name) {
        for (Iterator<Recipe> it = recipes.iterator(); it.hasNext(); ) {
            Recipe recipe = it.next();
            if (recipe.getName().equals(name)) {
                it.remove();
                return recipe;
            }
        }
        return null;
    }

    public List<String> searchByPrefix(String prefix) {
        List<String> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(recipe.getName());
            }
        }
        return result;
    }

    public Recipe getRecipeByName(String name) {
        for (Recipe recipe : recipes) {
            if (recipe.getName().equals(name)) {
                return recipe;
            }
        }
        return null;
    }

    public List<String> getAllRecipes() {
        List<String> allRecipeNames = new ArrayList<>();
        for (Recipe recipe : recipes) {
            allRecipeNames.add(recipe.getName());
        }
        return allRecipeNames;
    }
}

class Recipe {
    private final String name;
    private final List<String> ingredients;
    private final int cookingTime;  // in minutes
    private final String cuisine;

    public Recipe(String name, List<String> ingredients, int cookingTime, String cuisine) {
        this.name = name;
        this.ingredients = ingredients;
        this.cookingTime = cookingTime;
        this.cuisine = cuisine;
    }

    public String getName() {
        return name;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public String getCuisine() {
        return cuisine;
    }
}

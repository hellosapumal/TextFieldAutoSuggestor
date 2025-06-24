# AutoSuggestor Java Library

AutoSuggestor is a lightweight Java Swing component that adds an **auto-completion dropdown** (suggestions popup) to any `JTextField`. It fetches suggestions dynamically from a **MySQL (or any JDBC) database** table using a flexible multi-column search.

---

## Features

- Dynamic suggestions based on user input.
- Configurable search columns and table.
- Shows a popup list with matching results.
- Select by keyboard or mouse.
- Supports callback with selected item's ID and label.
- Easy to customize font and popup size.
- Simple to integrate with existing Swing + JDBC apps.

---

## Usage

1. Add the `AutoSuggestor` class to your project.
2. Prepare a JDBC `Connection` to your database.
3. Create an instance by passing:
   - The `JTextField` to enhance.
   - The DB connection.
   - Table name.
   - List of columns to search.
   - ID column name.
   - Optional icon (can be null).
   - Callback to receive selected value.

Example:

```java
AutoSuggestor autoSuggestor = new AutoSuggestor(
    myTextField,
    connection,
    "customers",
    Arrays.asList("name", "phone"),
    "customer_id",
    null,
    (id, label) -> System.out.println("Selected: " + id + " -> " + label)
);
````

---

## Customization

* Change font for suggestion list:

```java
autoSuggestor.setSuggestionFont(new Font("Arial", Font.BOLD, 14));
```

* Change popup size:

```java
autoSuggestor.setPopupSize(300, 200);
```

---

## Requirements

* Java 8+
* Swing UI
* JDBC compatible database (tested with MySQL)
* Database schema with searchable text columns and unique ID column

---

## Notes

* Debounce timer is set to 300ms to avoid querying on every keystroke immediately.
* SQL uses LIKE with wildcards to find matching text.
* Limits results to 10 entries by default.
* Customize as needed for performance or UI behavior.

---

## License

MIT License - Free to use and modify.

---

## Author

Your Name - \[[hellosapumal@gamil.com](mailto:hellosapumal@gamil.com)]

---

## Contact

Feel free to reach out for questions or contributions!



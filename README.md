# Bikeservice

This is a work in progress

## Running

Either run the application from the command line or from IntelliJ. 
It is possible to run the application enabled with auto reload
### Command line
* `./gradlew clean build run`
* For auto reload, open a new tab and enter `./gradlew -t installDist`

### IntelliJ
Run configurations are added in the git repository. You can simply run `bikeservice` for
hot reload. However, a description to create it on your own follows:

* Open `Application.kt` and hit the green play-button next to the `main()` function
* For auto reload, go to `Edit Configuration` (In the dropdown next to play and debugger icon up to the right)
   * The run configuration for the `main()` should appear under Kotlin. Select it and hit
   `Allow parallel run` in the top right corner
   * Hit `+` and create a new `Gradle` configuration with the following inputs.
        * Name: `AutoReload`
        * Gradle tasks:  `./gradlew`
        * Arguments: `-t installDist`
        * Allow parallel run by checking the box in the top right corner
   * Hit `+` and create a new `Compound` configuration and select the two configurations descriped above. 
   Lets call it `Bikeservice`
   
   * Run `Bikeservice`-configuration and the auto reload functionality is present 

### Ktlint
This project is using a code formatter called Ktlint.

* To check for format errors: `./gradlew ktlintCheck`
* To fix all format errors: `./gradlew ktlintFormat`

#### Pre-commit script
To run `ktlintFormat` as a pre-hook (recommended) do the following:
* `brew install ktlint`
* `./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook`


### Docker
- `docker build --tag bikeservice:0.0.1 .`
- `docker run --publish 8000:8080 --name bs bikeservice:0.0.1`

If the docker image already exists, you can remove it by: `docker rm --force bs`

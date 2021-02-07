# Slavicin Jidelna Api
Using selenium to order lunch in my school canteen.
I made an android app that is using this api!

## REST API usage
First, you must login with your canteen credentials. If you don't do that, all the endpoints will
give you HTTP error 401 Unauthorized. You have 10 attempts of incorrect passwords.
- /login
	- POST
		- needs request body: `{"username": "johndoe0", "password": "secretpassword420"}`
		- returns status code 200 and sets a `authid` cookie if everything went ok
[//]: # (Hello)

So now you have a magic cookie! Now you can use all other endpoints.
All endpoints should return HTTP code 200 if everything went out smoothly, otherwise it should
give you an http error with a nice message in response body.

- /menu
	- GET
		- returns list of days that have menus - CantryMenu ( see data models )
	- GET with query parameter date, for example date=2020-02-27
		- returns one CantryMenu for specified date
	- POST
		- used to order or cancel a dinner
		- needs request body, here is example:
		`{"action": "order"|"cancel", "date":"2020-05-26", "menuNumber":"1"}`
- /settings
	- GET
		- used to retrieve your settings, for example auto ordering settings
		- returns json of UserSettings (see data models)
	- POST
		- used to update your settings
		- request body: json of UserSettings (see data models)

### Data models
All serialized as JSON.
#### CantryMenu
```
Dinner(
    type: String,
    menuNumber: Int,
    name: String,
    allergens: List<Int>,
    status: String
)
CantryMenu(
    date: String,
	val menus: List<Dinner>
)
```

#### UserSettings
```
AllergensConfig(
    loveList: List<Int>,
    blackList: List<Int>
)

AutoorderConfig(
    randomOrders: Boolean,
    prefferedMenuNumber: Int,
    allergens: AllergensConfig,
    orderUncomplying: Boolean
)

AutoorderRequestConfig(
    orderAll: Boolean,
    orderDaysInAdvance: Int
)

AutoorderSetting(
    enable: Boolean,
    config: AutoorderConfig,
    requestConfig: AutoorderRequestConfig
)

UserSettings(
    autoorder: AutoorderSetting
)
```


## How it works

Cherrypy server does api things. The API requests comes to it, and it fullfils them by querying and
sending order requests atour canteen site with selenium.

### How it fullfils the basic tasks
It creates a work distributor (`BrowserWorkDistributor`) of n workers (you can specify that in
server.py). When a user sends an API request, it the cherrypy handler creates a `Job`, which
is then distributed using the `BrowserWorkDistributor`.

The distributor choses the best suitable worker for the job, by selecting one that doesn't have
anything to do. If all workers are doing something, it adds the job to the least busy worker's
job queue.

Each `Worker` fires up a firefox browser when initialized. It has a queue, so you can assign any
number of `Job` you want using the function `do_job`

Workers make use of the module `jidelna_webapp_handler`, which is just a helper that uses selenium
functions of a browser/webdriver you pass to it. It has the basic functions
of interaction with our canteen web app, like login, logout, get menu and such.

## Running it
### Prerequisities
- `python3`
	- comes by default in linux distros
- `postgresql` database
	- I have hardcoded some shitty credentials right into server.py
	- so make a database, if need to you can of course modify it in server.py
	- If you want to run it locally, you can just run the script setup-tools/db-create.sh to create database and user
- python modules: `cherrypy`, `selenium`, `schedule`
	- debian one liner: `sudo apt install python3-cherrypy3 python3-selenium python3-schedule`
- firefox
	- debian install: `sudo apt install firefox-esr`
- geckodriver
	- download the latest and add it to path
	- or just do `sudo cp setup-tools/geckodriver /usr/bin && sudo chmod 777 /usr/bin/geckodriver`

### Configuration
You must specify your ip adress/host name in `server.conf`.

### Run
just run server.py with python 3

`python3 server.py`

### Run at boot (systemd)
1. set the working dir in the example service at `setup-tools/jidelna.example.service`
2. `sudo cp setup-tools/jidelna-app.example.service /etc/systemd/system/jidelna-app.service`
3. `sudo systemctl daemon-reload`
4. `sudo systemctl enable jidelna-app.service`
5. `sudo systemctl start jidelna-app.service`
6. Look at the systemd logs. `sudo journalctl -U jidelna-app.service`

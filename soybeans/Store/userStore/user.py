class User:
    def __init__(self, id, username, password, authid, autoorder_enable, autoorder_settings, autoorder_request_settings,
                 autoorder_cancellation_dates, register_datetime):
        # Data class for storing info about a user
        self.id = id
        self.username = username
        self.password = password
        self.authid = authid
        self.autoorder_enable = autoorder_enable
        self.autoorder_settings = autoorder_settings
        self.autoorder_request_settings = autoorder_request_settings
        self.autoorder_cancellation_dates = autoorder_cancellation_dates
        self.register_datetime = register_datetime

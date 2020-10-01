import threading


class Job:
    # Data class for storing info about a task (Job). This is supplied to Worker
    def __init__(self, job_type, as_user, *argv):
        self.evt = threading.Event() # Used for determining if the job is finished
        self.type = job_type  # Enum value from Jobs
        self.user = as_user  # instance of User class
        self.arguments = argv
        self.result = None  # Can be an exception if something bad happened during the job fulfilling

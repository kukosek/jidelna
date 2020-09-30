import threading


class Job:
    def __init__(self, job_type, as_user, *argv):
        self.evt = threading.Event()
        self.type = job_type
        self.user = as_user
        self.arguments = argv
        self.result = None

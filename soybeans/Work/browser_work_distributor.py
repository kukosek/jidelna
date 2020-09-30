from Work.worker import Worker
import cherrypy


class BrowserWorkDistributor:
    def __init__(self, number_of_workers, cur):
        self.workers = []
        for n in range(number_of_workers):
            cherrypy.log("Starting browser " + str(n + 1) + "/" + str(number_of_workers))
            self.workers.append(Worker(cur))

    def distribute(self, job):
        target_worker = None
        insert_index = -1
        for worker in self.workers:
            if worker.loggedUser == job.user or worker.loggedUser is None:
                target_worker = worker
                break
        if target_worker is None:
            for worker in self.workers:
                i = 0
                for worker_job_in_queue in worker.callQueue:
                    if worker_job_in_queue.user == job.user:
                        target_worker = worker
                        insert_index = i + 1
                    i += 1
        if target_worker is None:
            queue_sizes = []
            for worker in self.workers:
                queue_sizes.append(len(worker.callQueue))
            target_worker = self.workers[queue_sizes.index(min(queue_sizes))]
        if insert_index == -1:
            return self.workers[0].do_job(job)
        else:
            return self.workers[0].do_job(job, insert_index)

    def close_all(self):
        for worker in self.workers:
            worker.browser.close()

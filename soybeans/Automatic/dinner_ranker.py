import random
import copy
import operator


class RankableDinner:
    def __init__(self, dinner):
        self.name = dinner.name
        self.menu_number = dinner.menu_number
        self.allergens = dinner.allergens

        self.rank = 0
        self.allergic = False


class DinnerRanker:
    # Initiate this with autoorder settings
    def __init__(self, user_autoorder_settings):
        self.settings = user_autoorder_settings

    # Then this guesses what dinner from the menu will the user like. Returns dinner number.
    def get_best_dinner_number(self, menu) -> int:
        r_menu = []
        rank_result_number = None
        if "random" in self.settings:
            if self.settings["random"]:
                rank_result_number = random.randint(1, len(menu))
        if rank_result_number is None:
            for dinner in menu:
                r_dinner = RankableDinner(dinner)
                r_menu.append(r_dinner)
                if "prefferedMenuNumber" in self.settings:
                    if self.settings["prefferedMenuNumber"] != "None":
                        if r_dinner.menu_number == self.settings["prefferedMenuNumber"]:
                            r_dinner.rank += 1
                if "allergens" in self.settings and self.settings["allergens"] is not None:
                    if "blacklist" in self.settings["allergens"] and self.settings["allergens"][
                        "blacklist"] is not None:
                        blacklist = self.settings["allergens"]["blacklist"]
                        for hatedAllergen in blacklist:
                            if hatedAllergen in r_dinner.allergens:
                                r_dinner.rank -= 10
                                r_dinner.allergic = True
                    if "lovelist" in self.settings["allergens"] and self.settings["allergens"]["lovelist"] is not None:
                        lovelist = self.settings["allergens"]["lovelist"]
                        for lovedAllergen in lovelist:
                            if lovedAllergen in r_dinner.allergens:
                                r_dinner.rank += 2

            dinners_by_rank = list(reversed(sorted(r_menu, key=operator.attrgetter('rank'))))
            order_uncomplying = True
            if "orderUncomplying" in self.settings:
                order_uncomplying = self.settings["orderUncomplying"]
            if order_uncomplying:
                rank_result_number = dinners_by_rank[0].menu_number
            else:
                for dinner in dinners_by_rank:
                    if dinner.allergic is False:
                        rank_result_number = dinner.menu_number
                        break
        return rank_result_number

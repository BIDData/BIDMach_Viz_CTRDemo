# BIDMach_Viz
Interactive Machine Learning and Visualization Tools
============================================================
class AdBiddingSimulation:
    def __init__(ad_model, user_model, quality_func):
       # TODO: pass in the quality function, or the parameters of    quality_func?
       ad_model, user_model - BIDMat FMat
       quality_func - a Scala function


    # main function for the simulation
    def simulate(keywords, bids):
        “””
   keywords - a Scala list of keyphrases that we need to simulate the process on.
        bids - a list of Scala mapping, where each mapping is for each keyword, from advertiser to their bids on this kw.
        returns a list of metric values, one per keyword
        “””
        # 1. get quality score for each advertiser
        # 2. sort to get rank
        # 3. for each (advertiser, rank):
                 get the user_CTR on it
        # 4. compute metrics using user_CTR
		


     def get_quality_score(advertiser, keyword, bid):
		“”Returns the Q score for a particular ad*kw and bid””

		CTR = (read from ad*kw row vector)
		Q = bid^B * CTR^a
		return Q

     def get_ranking(quality_scores):
	    ““”
         quality_scores - a mapping from advertiser to quality score

         return a mapping from advertiser to their ranking.
         “””
     

	def get_CTR(advertiser, keyword, rank):
	    “”Return CTR for a specific ad*kw pair and rank slot.””

		ad_kw_CTR = (read CTR from U)
		rank_CTR = (read CTR from V)
		return ad_kw_CTR * rank_CTR

	def calculate_profit(CTR, bid):
“”Return a list of expected profits per impression calculated by CTR and bid for each keyphrases. “”

expected_profit = CTR * bid
		return expected_profit

	def aggregate_metrics(metrics):
		“Returns the sum up a list of metrics.”
		return sum(metrics)

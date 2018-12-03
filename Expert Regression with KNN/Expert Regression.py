import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

series = "RFEDGOV"
cwd = "C:\\Users\\mazch\\Desktop\\Shit Hub\\SPFProcessing\\input\\"
min_obs = 5
no_history_penalty = 1
steps_ahead = 4
hold_out_steps = 20
variance_max = 0.2
histogram_length = 8
std_histogram = 4
max_window = 5
use_variance_adjusted = True
just_histograms = False
just_variance = False
final_result = True

# from testing
# NGDP ~ just variance adjusted, 5 min observations
# INPROD ~ 5, bias, variance and histogram
# UNEMP ~ 5, bias and variance adjusted

periods_ahead_dict = {0:"End_of_Current_Quarter",1:"Next_Quarter",2:"Next_Quarter+1",3:"Next_Quarter+2",4:"Next_Quarter+3"}
dfs = pd.read_csv(cwd+series+".csv")
truth = pd.read_csv(cwd+'Ground_Truth.csv')

agents = np.unique(dfs["ID"].values)
agents = np.append(agents,"Truth")

years = np.unique(dfs["YEAR"].values)
quarters = np.unique(dfs["QUARTER"].values)
time_stamps = np.array([], dtype='str')

def date2stamp(Y,Q):
    return str(Y) + str(Q)
    
def stamp2date(S):
    return int(S[:-1]), int(S[-1])
    
def periods_ahead(Y,Q,steps_ahead):
    k = steps_ahead
    while k>0:
        Q+=1
        if Q>4:
            Q-=4
            Y+=1
        k -=1
    return Y, Q

for i in years:
    for j in quarters:
        if len(dfs[dfs["YEAR"]==i][dfs["QUARTER"]==j])>0:
            stamp = date2stamp(i,j)
            y, q = stamp2date(stamp)
            y, q = periods_ahead(y,q,steps_ahead)   
            if truth[truth["Date"]==str(y)+":Q"+str(q)][series].count() > 0:        
                time_stamps = np.append(time_stamps,stamp)

if final_result:
    k = steps_ahead 
else:
    k = steps_ahead + hold_out_steps
while k>0:
    time_stamps=np.delete(time_stamps,-1)
    k-=1
agent_table = pd.DataFrame(index=agents,columns=time_stamps)

# Populate table by agent
for s in time_stamps:
    y, q = stamp2date(s)
    y_a, q_a = periods_ahead(y,q,steps_ahead)
    agent_table[s]["Truth"] = truth[truth["Date"]==str(y_a)+":Q"+str(q_a)][series].values[0]
    for a in agents[:-1]:
        pred = dfs.loc[dfs["YEAR"]==y]
        pred = pred.loc[dfs["QUARTER"]==q]
        pred = pred.loc[dfs["ID"]==int(a)][periods_ahead_dict[steps_ahead]]
        if len(pred) > 0:
            agent_table[s][a] = pred.values[0]
 
temp_stamps = time_stamps.copy()
for s in temp_stamps:
    if agent_table[s].count() <= 1:
        agent_table.drop(columns=[s],inplace=True)
        time_stamps=np.delete(time_stamps,np.where(time_stamps==s))
            
def evaluate(table,show_error,already_weighted):
    prediction = table.loc["Truth"].copy(deep=True)
    for s in time_stamps:
        if already_weighted:
            prediction[s] = np.sum(table.loc[agents[:-1]][s])
        else:
            prediction[s] = np.mean(table.loc[agents[:-1]][s])
    error = prediction - table.loc["Truth"]
    if final_result:
        error = error[time_stamps[-hold_out_steps:]]
    if show_error:
        plt.plot(error)
        plt.show()
    return np.sum(error**2)
        
print("Initial Peformance",evaluate(agent_table,False,False))

def window_bias_table(table,n):
    bias_table = table.copy(deep=True)
    adjusted_table = table.copy(deep=True)
    for s in time_stamps:
        for a in agents[:-1]:
            if not np.isnan(bias_table[s][a]):
                # only works for non-negative data
                bias_table[s][a] /= bias_table[s]["Truth"]
    bias_estimates = bias_table.copy(deep=True)
    for p in range(len(time_stamps)):
        s = time_stamps[p]
        prev_stamps = time_stamps[:max((p-1),0)]
        for a in agents[:-1]:
            preds = bias_table.loc[a][prev_stamps]
            n_preds = preds.count()
            # if there are n or less predictions, bias is estimated at 0
            if n_preds >= n:
                bias_estimates[s][a] = np.mean(preds)
                #bias_estimates[s][a] = np.exp(np.mean(np.log(preds)))
            else:
                #bias_estimates[s][a] = 0
                bias_estimates[s][a] = 1
            if not np.isnan(adjusted_table[s][a]):
                adjusted_table[s][a] = adjusted_table[s][a]/bias_estimates[s][a]
    return adjusted_table, bias_estimates

if not just_histograms:
    adjusted_table, bias_estimates = window_bias_table(agent_table,min_obs)
    print("After adjusting for bias",evaluate(adjusted_table,False,False))

def window_variance_table(table,n):
    variance_table = table.copy(deep=True)
    new_table = table.copy(deep=True)
    weighting_table = table.copy(deep=True)
    z_scores = table.copy(deep=True)
    for s in time_stamps:
        for a in agents[:-1]:
            if not np.isnan(variance_table[s][a]):
                variance_table[s][a] -= variance_table[s]["Truth"]
                variance_table[s][a] /= variance_table[s]["Truth"]
                variance_table[s][a] **= 2
                if variance_table[s][a] > variance_max:
                    variance_table[s][a] = variance_max
    sigma_estimates = variance_table.copy(deep=True)
    for p in range(len(time_stamps)):
        s = time_stamps[p]
        prev_stamps = time_stamps[:max((p-1),0)]
        var = table.loc[agents[:-1]][s]
        var_rel = var.copy()
        avg = np.mean(var)
        for a in agents[:-1]:
            if not np.isnan(table[s][a]):
                var_rel[a] -= avg
                var[a] -= avg
                var_rel[a] /= avg
                var[a] **= 2
                var_rel[a] **= 2
        var_rel = np.mean(var_rel)
        default_sigma = no_history_penalty/var_rel
        var = np.sqrt(np.mean(var))
        for a in agents[:-1]:
            if not np.isnan(z_scores[s][a]):
                z_scores[s][a] -= avg
                z_scores[s][a] /= var
        for a in agents[:-1]:
            preds = variance_table.loc[a][prev_stamps]
            n_preds = preds.count()
            # if there are m or less predictions, variance is undefined
            if not np.isnan(variance_table[s][a]):
                if n_preds >= n:
                    # sample variance is calculated ( n must be 1 or greater )
                    sigma_estimates[s][a] = 1/np.mean(preds)
                else:
                    sigma_estimates[s][a] = default_sigma
    for s in time_stamps:
        total_sigma = np.sum(sigma_estimates.loc[agents[:-1]][s])
        total_count = new_table.loc[agents[:-1]][s].count()
        for a in agents[:-1]:
            if not np.isnan(new_table[s][a]):
                    if total_sigma == 0:
                        weighting_table[s][a] = 1/total_count
                        new_table[s][a] *=  weighting_table[s][a]
                    else:
                        weighting_table[s][a] = (sigma_estimates[s][a]/total_sigma)
                        new_table[s][a] *= weighting_table[s][a]
    return new_table, sigma_estimates, weighting_table, z_scores
    
if just_histograms:
    weighted_table, sigma_estimates, weighting_table, z_scores = window_variance_table(agent_table,min_obs)
else:
    weighted_table, sigma_estimates, weighting_table, z_scores = window_variance_table(adjusted_table,min_obs)

print("With adjustment for variance",evaluate(weighted_table,True,True))

if not just_variance:

    histograms = {}
    error_multiplier = {}
    prediction = {}

    for s in time_stamps:
        vals = np.array([])
        freqs = np.array([])
        for a in agents[:-1]:
            if not np.isnan(z_scores[s][a]):
                vals = np.append(vals,z_scores[s][a])
                freqs = np.append(freqs,weighting_table[s][a])
        histogram = [0] * histogram_length
        for i in range(len(vals)):
            if vals[i] < -std_histogram:
                if use_variance_adjusted:
                    histogram[0] = histogram[0] + freqs[i]
                else:
                    histogram[0] = histogram[0] + 1
            elif vals[i] > std_histogram:
                if use_variance_adjusted:
                    histogram[0] = histogram[0] + freqs[i]
                else:
                    histogram[-1] = histogram[-1] + 1
            else:
                v_new = int(np.round((vals[i] + std_histogram)*(histogram_length-1)/(2*std_histogram)))
                if use_variance_adjusted:
                    histogram[v_new] = histogram[v_new] + freqs[i]
                else:
                    histogram[v_new] = histogram[v_new] + 1
        histogram = np.array(histogram)
        histogram = histogram/np.sqrt(np.dot(histogram,histogram))
        histograms[s] = histogram
        if use_variance_adjusted:
            prediction[s] = np.sum(weighted_table.loc[agents[:-1]][s])
        else:
            prediction[s] = np.mean(adjusted_table.loc[agents[:-1]][s])
        error_multiplier[s] = adjusted_table[s]["Truth"]/prediction[s]
    
    def KNN_Regression(histograms,error_multiplier,prediction):
        for p in range(len(time_stamps)):
            s = time_stamps[p]
            current_histo = histograms[s]
            past_histograms = {}
            past_distances = {}
            for i in range(max(p-1,0)):
                q = time_stamps[i]
                new_histo = histograms[q]
                #dst = np.sum((current_histo - new_histo)**2)
                #print(dst)
                dst = 1 - np.dot(current_histo,new_histo)
                dst = 1 / dst
                if len(past_histograms.keys()) <= max_window:
                    past_distances[dst] = q
                else:
                    min_dist = np.min(list(past_distances.keys()))
                    if dst > min_dist:
                        del past_histograms[past_distances[min_dist]]
                        del past_distances[min_dist]
                        past_distances[dst] = q
            total_dst = np.sum(list(past_distances.keys()))
            if total_dst>0:
                estimate = 0
                for v in  past_distances.keys():
                    #print(total_dst)
                    estimate += v/total_dst*error_multiplier[past_distances[v]]
                prediction[s] *= estimate
        return prediction
    
    prediction = KNN_Regression(histograms,error_multiplier,prediction)
    
    errors = {}
    if final_result:
        for s in time_stamps[-hold_out_steps:]:
            errors[s] = prediction[s] - adjusted_table[s]["Truth"]
            errors[s] **= 2
        print('After histogram adjusting',np.sum(list(errors.values())))
    else:
        for s in time_stamps:
            errors[s] = prediction[s] - adjusted_table[s]["Truth"]
            errors[s] **= 2
        print('After histogram adjusting',np.sum(list(errors.values())))
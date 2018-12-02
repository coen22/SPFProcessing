import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from scipy.stats import shapiro # Shapiro-Wilk
from scipy.stats import anderson # Anderson-Darling
from scipy.stats import normaltest # D’Agostino
from matplotlib.animation import FuncAnimation

series = "NGDP"
cwd = "C:\\Users\\mazch\\Desktop\\Shit Hub\\SPFProcessing\\input\\"
n = 9 # window size for bias
m = 9 # window size for variance
steps_ahead = 3
save_mp4 = False

## Set up bias and variance tables

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

k = steps_ahead
while k>0:
    time_stamps=np.delete(time_stamps,-1)
    k-=1
agent_table = pd.DataFrame(index=agents,columns=time_stamps)

# Populate table by agent
# Populate table by agent
for s in time_stamps:
    y, q = stamp2date(s)
    y, q = periods_ahead(y,q,steps_ahead)
    agent_table[s]["Truth"] = truth[truth["Date"]==str(y)+":Q"+str(q)][series].values[0]
    for a in agents[:-1]:
        pred = dfs[dfs["YEAR"]==y][dfs["QUARTER"]==q][dfs["ID"]==int(a)][periods_ahead_dict[steps_ahead]]
        if len(pred) > 0:
            agent_table[s][a] = pred.values[0]
    
# Populate estimates of bias table according to window size n
def window_bias_table(agent_table,n):
    bias_table = agent_table.copy()
    for p in range(len(time_stamps)):
        s = time_stamps[p]
        for a in agents[:-1]:
            if not np.isnan(bias_table[s][a]):
                bias_table[s][a] -= bias_table[s]["Truth"]
                bias_table[s][a] /= bias_table[s]["Truth"]
    bias_estimates = bias_table.copy()
    for p in range(len(time_stamps)):
        s = time_stamps[p]
        prev_stamps = time_stamps[:max((p-1),0)]
        for a in agents[:-1]:
            preds = bias_table.loc[a][prev_stamps]
            # if there are n or less predictions, bias is estimated at 0
            if preds.count() > n:
                bias_estimates[s][a] = np.mean(preds)
            else:
                bias_estimates[s][a] = 0
    return bias_table, bias_estimates
    
bias_table, bias_estimates = window_bias_table(agent_table,n)

# Populate estimates of bias table according to window size m
# (needs more than m previous samples to make an estimate)
def window_variance_table(agent_table,bias_estimates,m):
    variance_table = agent_table.copy()
    z_scores = bias_estimates.copy()
    for p in range(len(time_stamps)):
        s = time_stamps[p]
        for a in agents[:-1]:
            if not np.isnan(bias_table[s][a]):
                variance_table[s][a] -= variance_table[s]["Truth"]
                variance_table[s][a] /= variance_table[s]["Truth"]
                variance_table[s][a] **= 2
                if np.sqrt(variance_table[s][a]) > 0.5:
                    print("Agent",a,"at time stamp",s,"looks a bit high.")
    sigma_estimates = variance_table.copy()
    j=0
    for p in range(len(time_stamps)):
        s = time_stamps[p]
        prev_stamps = time_stamps[:max((p-1),0)]
        for a in agents[:-1]:
            preds = variance_table.loc[a][prev_stamps]
            n_preds = preds.count()
            # if there are m or less predictions, variance is undefined
            if n_preds > m:
                # sample variance is calculated ( m must be 1 or greater )
                sigma_estimates[s][a] = np.sqrt(np.sum(preds)/(n_preds-1))
                z_scores[s][a] *= np.sqrt(n_preds)/sigma_estimates[s][a]
            else:
                sigma_estimates[s][a] = np.nan
                z_scores[s][a] = np.nan
    return variance_table, sigma_estimates, z_scores
        
variance_table, sigma_estimates, z_scores = window_variance_table(agent_table,bias_estimates,m)

## Compute z scores for each agents with enough data


bias_z_scores = []
s = time_stamps[-1]
for a in agents[:-1]:
    if not np.isnan(z_scores[s][a]):
        alpha = z_scores[s][a]
        bias_z_scores.append(alpha)

## Add labels to plots
fig = plt.figure()
sns.distplot(bias_z_scores, bins=20, kde=False, rug=True)
plt.title("Z scores of realtive errors in " + series + "\n series according to estimated Bias and Variance")
plt.xlabel("Z score of agent")
plt.ylabel("Frequency")
plt.show()

relative_errors = []
sigmas = []

for s in time_stamps:
    for a in agents[:-1]:
        if (not np.isnan(sigma_estimates[s][a])) and (not np.isnan(agent_table[s][a])):
            relative_errors.append(np.sqrt(variance_table[s][a]))
            sigmas.append(sigma_estimates[s][a])
            
plot_data = pd.DataFrame()
plot_data["Sample Standard Deviation"] = sigmas
plot_data["Actual relative error"] = relative_errors
sns.jointplot(x="Sample Standard Deviation", y="Actual relative error", data=plot_data,kind="kde")
#plt.title("Estimated standard deviation of error in " + series + " versus actual standard deviation")
plt.show()

# plot for just last prediction made by experts


relative_errors = {}
sigmas = {}

for s in time_stamps:
    for a in agents[:-1]:
        if (not np.isnan(sigma_estimates[s][a])) and (not np.isnan(agent_table[s][a])):
            relative_errors[a] = np.sqrt(variance_table[s][a])
            sigmas[a] = sigma_estimates[s][a]
            
plot_data = pd.DataFrame()
plot_data["Sample Standard Deviation"] = sigmas.values()
plot_data["Actual relative error"] = relative_errors.values()
sns.jointplot(x="Sample Standard Deviation", y="Actual relative error", data=plot_data)
#plt.title("Estimated standard deviation of error in " + series + " versus actual standard deviation for last prediction made by each agent")
plt.show()

def create_histogram_data(agent_table):
    histogram_table = agent_table.copy()
    histogram_data = {}
    max_score = -np.inf
    min_score = np.inf
    for s in time_stamps:
        scores = np.array([])
        avg = np.mean(histogram_table[s][agents[:-1]])
        sd = np.std(histogram_table[s][agents[:-1]])
        for a in agents[:-1]:
            if not np.isnan(histogram_table[s][a]):
                histogram_table[s][a] -= avg
                histogram_table[s][a] /= sd
                if histogram_table[s][a] < min_score:
                    min_score = histogram_table[s][a]
                if histogram_table[s][a] > max_score:
                    max_score = histogram_table[s][a]
                scores=np.append(scores,histogram_table[s][a])
        histogram_data[s] = scores
    return histogram_data, max_score, min_score

histogram_data, max_score, min_score = create_histogram_data(agent_table)
max_score += 0.01
min_score -= 0.01

shapiro_p = {}
agostino_p = {}
for s in time_stamps:
    if len(histogram_data[s]) >= 3:
        shapiro_p[s] = shapiro(histogram_data[s])[1]
        agostino_p[s] = normaltest(histogram_data[s])[1]
    else:
        shapiro_p[s] = 1
        agostino_p[s] = 1
    
df = pd.DataFrame(index=time_stamps,columns=["Shapiro p value","Agostino p value"])
for s in time_stamps:
    df.loc[s]["Shapiro p value"] = shapiro_p[s]
    df.loc[s]["Agostino p value"] = agostino_p[s]
    
ax = df.plot(x=time_stamps, y="Shapiro p value", legend=True)
ax.legend(bbox_to_anchor=(1.0, 0.92),title="p from normal distribution")
df.plot(x=time_stamps, y="Agostino p value", ax=ax, legend=True, color="r")
plt.show()

fig, ax = plt.subplots()
fig.set_tight_layout(True)

print('fig size: {0} DPI, size in inches {1}'.format(
    fig.get_dpi(), fig.get_size_inches()))


def update(i):
    label = 'timestep {0}'.format(i)
    fig.clear()
    print(i)
    if len(histogram_data[i])==0:
        return
    plot=sns.distplot(histogram_data[i],kde=True,color='orange')
    plt.legend(bbox_to_anchor=(0.65, 0.85), loc=2, borderaxespad=0.)
    p1 = shapiro_p[i]
    p1*=100
    if p1<0.1:
        p1="<0.1%"
    else:
        p1="{:.1f}%".format(p1)
    p2=agostino_p[i]
    p2*= 100
    if p2<0.1:
        p2="<0.1%"
    else:
        p2="{:.1f}%".format(p2)
    result = anderson(histogram_data[i])
    p3=">15%"
    for j in range(1,len(result.critical_values)+1):
        if result.statistic > result.critical_values[-j]:
            p3 = "<"+str(result.significance_level[-j])+"%"
            break
    y,q = stamp2date(i)
    plt.annotate(str(y) + ' Quarter ' + str(q) + '\n' + 'Shapiro p-val '+p1 + "\n" + 'D\'Agostino p-val '+p2 + "\n" + 'Anderson p-val '+p3,(0.75,0.85))
    plt.ylim((0,1.2))
    plt.xlim((min_score,max_score))
    plt.xlabel('Standard Devations from mean estimate')
    plt.ylabel('Frequency Density')
    plt.title('Distribution of SPF estimates for '+series+' over time')
    return

if __name__ == '__main__':
    anim = FuncAnimation(fig, update, frames=time_stamps, interval=200,repeat=True)
    if save_mp4:
        anim.save(cwd + 'test.mp4', dpi=160, writer='imagemagick')
    else:
        plt.show()
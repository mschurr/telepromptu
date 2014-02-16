from fuzzy import Soundex
import numpy as np
soundex = Soundex(4)




def score(word1, word2):
	if word1 == "-" or word2 == "-":
		return 0
	return soundex(word1) == soundex(word2)


def pairwise_matrix_cell(i,j,X,Y,S,P,M):
    """
    Computes the cell values required for matrices S and P respectively
    using the scoring matrix M. For the global_pairwise function and the
    local_pairwise function.
    where S is the score-alignment matrix and P is the pointer matrix.
    X and Y are the sequences.
    """
    if i == 0:
        if j == 0:
            S[i][j] = 0                          #Dash corner case
        else:
            S[i][j] = S[i][j-1] + score("-",Y[j])#M['-'][Y[j]]     #Top-edge boundary case, align with dash
            P[i][j] = 0
    elif j == 0:
        S[i][j] = S[i-1][j] + score(X[i],"-")#M[X[i]]['-']        #Left-edge boundary case, align with dash
        P[i][j] = 90
    else:
        opt1 = S[i-1][j-1] + score(X[i],Y[j])#M[X[i]][Y[j]]      #Score of aligning A[i] with B[j]
        opt2 = S[i-1][j] + score(X[i],"-")#M[X[i]]['-']           #Score of aligning A[i] with '-'
        opt3 = S[i][j-1] + score("-",Y[j])#M['-'][Y[j]]           #Score of aligning B[i] with '-'
        S[i][j] = max([opt1,opt2,opt3])     #Computes maximal scoring alignment
        if S[i][j] == opt1:
            P[i][j] = 45                    #Point towards top left
        elif S[i][j] == opt2:
            P[i][j] = 90                    #Point upwards
        else:
            P[i][j] = 0                     #Point left

def pairwise_traceback(i,j,X,Y,S,P,mode):
    """
    Trace the optimal alignment Xprime and Yprime using the sequences X and Y
    from the function global_pairwise_alignment or the local_pairwise_alignment.
    
    Traceback is done using the optimal alignment score matrix S and the pointer matrix
    P. i,j points to the starting point of the traceback.
    
    Traceback terminates at the top left corner if mode=="global" and terminates at a
    0 entry in s if mode=="local"
    """
    #Traceback to construct the sequence
    Xprime = []
    Yprime = []
    while (mode=="global" and (i != 0 or j != 0)) or (mode=="local" and S[i][j] != 0):
        if P[i][j] == 45:
            Xprime = [X[i]] + Xprime        #Align A[i] with B[j] and traverse to the upper-left cell
            Yprime = [Y[j]] + Yprime
            i-=1
            j-=1
        elif P[i][j] == 90:
            Xprime = [X[i]] + Xprime        #Align A[i] with a dash, and traverse to the left cell
            Yprime = ['-'] + Yprime
            i-=1
        elif P[i][j] == 0:
            Xprime = ['-'] + Xprime
            Yprime = [Y[j]] + Yprime        #Align B[j] with a dash, and traverse to the upper cell
            j-=1
    return Xprime,Yprime

def global_pairwise_alignment(X,Y,M):
    """
    Input: Strings X and Y over alphabet Sigma and scoring matrix M of dimensions
    (|Sigma|+1)x(|Sigma|+1).

    M[0,j] for any j > 0 should define the score of aligning letter j of Y with dash.
    M[i,0] for any i > 0 should define the score of aligning letter i of X with dash.
    M[0,0] will not be used so it's value does not matter.
    
    Output: Strings Xprime and Yprime, and Maximal Score that are an Alignment of X and Y respectively where score
    (as defined by M) is maximal among all possible alignments of X and Y. 
    """
    A = [' '] + X#Empty space aligns the positions of A and B with the matrix S, as the 0th row and column represent alignments with dashes
    B = [' '] + Y
    m,n = len(A),len(B)
    S = np.zeros((m,n))         #S is a matrix of dimensions mxn that contains the optimal scores of alignments
    P = np.zeros((m,n))         #P is a matrix of dimensions mxn whose cells point to the predecessor cell for traceback, each cell can either be 0 (left), 45 (upper left), or 90 (up)
    #Compute all the values of the matrix S by traversing accross the matrix from top-left to bottom-right
    for i in range(m):
        for j in range(n):
            pairwise_matrix_cell(i,j,A,B,S,P,M)
    #Traceback to construct the encoded sequence alignment
    i,j = m-1,n-1       #i,j point to the bottom right of the matrix
    Xprime,Yprime = pairwise_traceback(i,j,A,B,S,P,"global")
    return Xprime,Yprime,S[m-1][n-1]


"""
A0 = "ACCT"
B0 = "TACGGT"
M = scoring_matrix(6,2,-4,Sigma)
print global_pairwise_alignment(A0, B0, M)
"""



#A0 = ["A","C","C","T"]
#B0 = ["T","A","C","G","G","T"]
A0 = """ Spent my days with a woman unkind, Smoked my stuff and drank all my wine. 
Made up my mind to make a new start, Going To California with an aching in my heart. 
Someone told me there's a girl out there with love in her eyes and flowers in her hair. 
Took my chances on a big jet plane, never let them tell you that they're all the same. 
The sea was red and the sky was grey, wondered how tomorrow could ever follow today. 
The mountains and the canyons started to tremble and shake 
as the children of the sun began to awake """.split(" ")
B0 = """ see was read and was grey """.split(" ")
r0, r1, s = global_pairwise_alignment(A0, B0, [])

for i in xrange(len(r0)):
	print r0[i], r1[i]
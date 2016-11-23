% MZN file model for IEC 61499 function block ECC induction from scenarios
% Author: Daniil Chivilikhin (chivdan@rain.ifmo.ru)

% given constants and domains

int: C; % machine size
int: V; % scenarios tree size
int: E; % input events count
int: X; % unique input variables sets count
int: O; % output events count
int: L; % output variables count

set of int: colors = 1..C+1;
set of int: vertices = 1..V;
set of int: input_events = 1..E+1;
set of int: guards = 1..X;
set of int: output_events = 1..O+1;
set of int: output_variables = 1..L;
set of int: zero_one = 0..1;

%% given data

array [vertices] of input_events: tree_input_event;
array [vertices] of guards: tree_guards;
array [vertices] of output_events: tree_output_event;
array [vertices] of vertices: tree_previous_active;
array [vertices, output_variables] of zero_one: tree_z;

%% variables

array [vertices] of var colors: x; % vertices colors
array [colors, input_events, guards] of var colors: y; % transitions destinations
array [colors] of var output_events: o; % output events 
array [colors, output_variables] of var zero_one: d_0; % what to do with zero
array [colors, output_variables] of var zero_one: d_1; % what to do with one 

array [colors, colors] of var bool: tr;
array [2..C] of var colors: p;
array [colors, colors] of var int: m;


%% constraints

% start vertex corresponds to start state
constraint x[1] = 1;

constraint forall (j in output_variables) (
    d_0[1, j] = 0 
);

constraint forall (j in output_variables) (
    d_1[1, j] = 1 
);

% output event constraints
constraint forall (v in 2..V where tree_output_event[v] != O+1) ( 
    (x[v] != 1) -> (o[x[v]] = tree_output_event[v])
);

constraint o[1] = O+1;
constraint o[C+1] = O+1;

constraint forall (c in 1..C+1) (
   o[c] = O+1 -> forall (j in output_variables) (d_0[c, j] = 0 /\ d_1[c, j] = 1)
);

constraint forall (v in 2..V, e in input_events, g in guards) (
    y[C+1, e, g] = C+1
);


constraint forall (v in 2..V where tree_output_event[v] == O+1 /\ tree_input_event[v] != E+1) (
    x[v] = C+1
);

% transitions constraints 
constraint forall (v in 2..V where tree_input_event[v] != E+1 /\ tree_output_event[v] != O+1) (
    y[x[tree_previous_active[v]], tree_input_event[v], tree_guards[v]] = x[v] 
    /\ (forall (gg in guards) (y[x[tree_previous_active[v]], E+1, gg] = C+1))
);

constraint forall (v in 2..V where tree_output_event[v] == O+1) (
    y[x[tree_previous_active[v]], tree_input_event[v], tree_guards[v]] = C+1
    /\ (forall (gg in guards) (y[x[tree_previous_active[v]], E+1, gg] = C+1))
);

constraint forall (v in 2..V where tree_input_event[v] == E+1 /\ tree_output_event[v] != O+1) (    
    forall (gg in guards) (y[x[tree_previous_active[v]], E+1, gg] = x[v])  
    /\ forall (e in 1..E, gg in guards) (y[x[tree_previous_active[v]], e, gg] = C+1)
    /\ forall (c in colors, e in 1..E+1, gg in guards) (
        (c != x[tree_previous_active[v]]) -> y[c, e, gg] != x[v])
);


% redundant constraints to unify y variables
% used to find all solutions
constraint forall (c in 1..C, e in 1..E+1, g in guards) (
    ((not exists (v in 2..V) (tree_input_event[v] == e /\ tree_guards[v] == g /\ x[tree_previous_active[v]] == c)) /\  (not exists (v in 2..V) (tree_input_event[v] == E+1 /\ x[tree_previous_active[v]] == c))) -> y[c, e, g] = C+1
);

% output algorithms constraints
constraint forall (v in 2..V, j in output_variables where tree_output_event[v] != O+1) (
    (tree_z[tree_previous_active[v], j] == 0) -> (d_0[x[v], j] = tree_z[v, j])
);

constraint forall (v in 2..V, j in output_variables where tree_output_event[v] != O+1) (
    (tree_z[tree_previous_active[v], j] == 1) -> (d_1[x[v], j] = tree_z[v, j])
);

% fix unused variables
constraint forall (j in output_variables, c in 1..C) (
    (not exists (v in 2..V) (tree_z[tree_previous_active[v], j] == 1 /\ x[v] == c) -> d_1[c, j] = 1)
);

constraint forall (j in output_variables, c in 1..C) (
    (not exists (v in 2..V) (tree_z[tree_previous_active[v], j] == 0 /\ x[v] == c) -> d_0[c, j] = 0)
);  

% transition definition
constraint forall (i in 1..C, j in 1..C) (
    if i < j then
        tr[i, j] = exists (e in 1..E+1, g in guards) (y[i, e, g] = j)
    else
        tr[i, j] = false
    endif
);

% parent definition
constraint forall (i in colors, j in colors where i < j) (
    (p[j] = i) <-> (tr[i, j] /\ (forall (k in 1..i-1) (not tr[k, j])))
);

% next state i parent is not smaller than our i-1
constraint forall (i in 3..C) (
    p[i - 1] <= p[i]
);

constraint forall (i in 1..C, j in 1..C+1, e in 1..E, g in guards) (
    if i < j then
        (tr[i, j] -> (
            (m[i, j] = g) <-> (y[i, e, g] = j /\ (forall (gg in 1..g-1) (y[i, e, gg] != j)))
        ))
        /\
        ((not tr[i, j]) -> m[i, j] = 1)
    else
       m[i, j] = 0
    endif
);


% ordering nodes i and i+1 with the same parent j
constraint forall (i in 1..C, j in 1..C where i < j - 1) (
   (((p[j - 1] = i) /\ (p[j] = i)) -> (m[i, j - 1] < m[i, j]))
);



solve satisfy;                          

output ["# x = ", show(x), "\n",
        "# y = ", show(y), "\n",
        "# o = ", show(o), "\n",
        "# d_0 = ", show(d_0), "\n", 
        "# d_1 = ", show(d_1), "\n"];

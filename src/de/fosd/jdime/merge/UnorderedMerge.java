/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.merge;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class UnorderedMerge<T extends Artifact<T>> implements MergeInterface<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(UnorderedMerge.class));
	private String logprefix;

	/**
	 * TODO: this needs high-level documentation. Probably also detailed documentation.
	 *
	 * @param operation
	 * @param context
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeTriple<T> triple = operation.getMergeTriple();
		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();
		T target = operation.getTarget();
		logprefix = operation.getId() + " - ";

		assert (left.matches(right));
		assert (left.hasMatching(right)) && right.hasMatching(left);

		if (LOG.isTraceEnabled()) {
			LOG.trace(prefix() + this.getClass().getSimpleName() + ".merge("
					+ left.getId() + ", " + base.getId() + ", " + right.getId()
					+ ")");
		}

		Revision l = left.getRevision();
		Revision b = base.getRevision();
		Revision r = right.getRevision();
		Iterator<T> leftIt = left.getChildren().iterator();
		Iterator<T> rightIt = right.getChildren().iterator();

		boolean leftdone = false;
		boolean rightdone = false;
		T leftChild = null;
		T rightChild = null;

		if (leftIt.hasNext()) {
			leftChild = leftIt.next();
		} else {
			leftdone = true;
		}
		if (rightIt.hasNext()) {
			rightChild = rightIt.next();
		} else {
			rightdone = true;
		}

		while (!leftdone || !rightdone) {
			if (!leftdone && !r.contains(leftChild)) {
				assert (leftChild != null);

				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(leftChild) + "is not in right");
				}
				if (b != null && b.contains(leftChild)) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(leftChild) + "was deleted by right");
					}
					// was deleted in right
					if (leftChild.hasChanges()) {
						// insertion-deletion-conflict
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(leftChild)
									+ "has changes in subtree.");
						}
						ConflictOperation<T> conflictOp = new ConflictOperation<>(
								leftChild, leftChild, null, target);
						conflictOp.apply(context);
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<>(
								leftChild, null);
						delOp.apply(context);
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(leftChild) + "is a change");
					}
					// leftChild is a change

					// FIXME: check if this can also be a conflict
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(leftChild) + "adding change");
					}
					// add the left change
					AddOperation<T> addOp = new AddOperation<>(leftChild,
							target, true);
					leftChild.setMerged(true);
					addOp.apply(context);
				}

				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					leftdone = true;
				}
			}

			if (!rightdone && !l.contains(rightChild)) {
				assert (rightChild != null);

				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(rightChild) + "is not in left");
				}
				if (b != null && b.contains(rightChild)) {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(rightChild) + "was deleted by left");
					}

					// was deleted in left
					if (rightChild.hasChanges()) {
						if (LOG.isTraceEnabled()) {
							LOG.trace(prefix(rightChild)
									+ "has changes in subtree.");
						}
						// insertion-deletion-conflict
						ConflictOperation<T> conflictOp = new ConflictOperation<>(
								rightChild, null, rightChild, target);
						conflictOp.apply(context);
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<>(
								rightChild, null);
						delOp.apply(context);
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(rightChild) + "is a change");
					}
					// rightChild is a change

					// FIXME: check if this can also be a conflict
					if (LOG.isTraceEnabled()) {
						LOG.trace(prefix(rightChild) + "adding change");
					}
					// add the right change
					AddOperation<T> addOp = new AddOperation<>(rightChild,
							target, false);
					rightChild.setMerged(true);
					addOp.apply(context);
				}

				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					rightdone = true;
				}
			} else if (l.contains(rightChild) && r.contains(leftChild)) {
				assert (leftChild != null);
				assert (rightChild != null);

				// left and right have the artifact. merge it.
				if (LOG.isTraceEnabled()) {
					LOG.trace(prefix(leftChild) + "is in both revisions, ["
							+ rightChild.getId() + "] too");

				}

				// merge left
				if (!leftChild.isMerged()) {
					Matching<T> mRight = leftChild.getMatching(r);
					T rightMatch = mRight.getMatchingArtifact(leftChild);

					// determine whether the child is 2 or 3-way merged
					Matching<T> mBase = leftChild.getMatching(b);

					MergeType childType = mBase == null ? MergeType.TWOWAY
							: MergeType.THREEWAY;
					T baseChild = mBase == null ? leftChild.createEmptyDummy()
							: mBase.getMatchingArtifact(leftChild);
					T targetChild = target == null ? null : target
							.addChild(leftChild);

					MergeTriple<T> childTriple = new MergeTriple<>(childType,
							leftChild, baseChild, rightMatch);

					MergeOperation<T> mergeOp = new MergeOperation<>(
							childTriple, targetChild);

					leftChild.setMerged(true);
					rightMatch.setMerged(true);
					mergeOp.apply(context);
				}

				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					leftdone = true;
				}

				// merge right
				if (!rightChild.isMerged()) {
					Matching<T> mLeft = rightChild.getMatching(l);
					T leftMatch = mLeft.getMatchingArtifact(rightChild);

					// determine whether the child is 2 or 3-way merged
					Matching<T> mBase = rightChild.getMatching(b);

					MergeType childType = mBase == null ? MergeType.TWOWAY
							: MergeType.THREEWAY;
					T baseChild = mBase == null ? rightChild.createEmptyDummy()
							: mBase.getMatchingArtifact(rightChild);
					T targetChild = target == null ? null : target
							.addChild(rightChild);

					MergeTriple<T> childTriple = new MergeTriple<>(childType,
							leftMatch, baseChild, rightChild);

					MergeOperation<T> mergeOp = new MergeOperation<>(
							childTriple, targetChild);

					leftMatch.setMerged(true);
					rightChild.setMerged(true);
					mergeOp.apply(context);
				}

				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					rightdone = true;
				}
			}

			if (LOG.isTraceEnabled() && target != null) {
				LOG.trace(prefix()
						+ "target.dumpTree() after processing child:");
				System.out.println(target.dumpRootTree());
			}
		}
	}

	/**
	 * Returns the logging prefix.
	 *
	 * @return logging prefix
	 */
	private String prefix() {
		return logprefix;
	}

	/**
	 * Returns the logging prefix.
	 *
	 * @param artifact
	 *            artifact that is subject of the logging
	 * @return logging prefix
	 */
	private String prefix(final T artifact) {
		return logprefix + "[" + artifact.getId() + "] ";
	}
}
